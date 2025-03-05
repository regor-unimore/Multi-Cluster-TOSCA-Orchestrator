# EXAMPLE: Optimization Pipeline1

This guide will show you how you can use TORCH-V2 to deploy an application consisting of multiple docker-containerized microservices in two different Kubernetes Cluster federated with Liqo. The first cluster (K8s) contains a private OSRM server which is required by one of the microservices (dm) and also contains more computational power, making it more suitable to run cpu-intensive application. The second cluster (k3s) is instead less powerful, making it more suitable for lightweight workload. Because of this asymmetry, we decide to use explicit relationship inside the TOSCA template to influence the positioning of the microservices. 

## Prerequisites
- To follow this guide you need to create two Kubernetes Cluster (1.28+) if you don't already have them available. You can create a local cluster using KinD on your pc or using kubeadm on two or more VMs or physical nodes. Check our [KinD guide](../../doc/cluster-guide/KinD/kind-guide.md) to learn how you can create a local Kubernetes cluster using docker. A guide for baremetal Kubernetes with kubeadm will be available in the future.
- [kubectl](https://kubernetes.io/docs/tasks/tools/#kubectl) installed and setted up to reach your cluster
- [Docker](https://docs.docker.com/engine/install/) installed and, if you are using a Linux distro, make sure to set docker to be runnable [without root](https://docs.docker.com/engine/install/linux-postinstall/#manage-docker-as-a-non-root-user)

NOTE: The commands reported in this guide presume you are using bash to run them. For other type of shell the syntax may differ.  

NOTE2: Due to current limitations, you need to create the Liqo Federation manually and have the cluster already in a peering session. The first "cluster" mentioned in the Federation node must be the entrypoint of the federation (the Client). 

## Create a RBAC Role for TORCH-V2

The current version of TORCH-V2 requires some authorization to operate on each cluster. When using Liqo, TORCH_V2 need to have the authorization required to install Liqo on the each cluster (if not already installed) and to create new namespaces to offload other than the usual capability of managing "deployments", "pods" and "services". For semplicity, we will create a user for TORCH_V2 with the same authorization as the cluster admin but of course it is possible to create a more fine tuned user as long as all the authorization required are given.   

First of all, enable (if it's not already enabled) RBAC on your cluster. KinD already enable it by default so if you are testing TORCH with it no further operation are required.  
You can check if RBAC is enabled on a general Kubernetes cluster by listing the api-server extension enabled and searching for `rbac.authorization.k8s.io`.  
I.e;  
`kubectl api-versions | grep "rbac.authorization.k8s.io/v1"`  

Then we need to crete a Role with the authorization for TORCH-V2. The file role-torch.yaml in the folder *examples/RVRPTW-and-compactor/resources* already contains the correct authorization, we can apply them to the cluster:  
`kubectl apply -f role-torch-admin.yaml`  

We now associate the Role with the authorization to a username that from now on will represents TORCH-V2 as a user. The file role-binding-torch.yaml show an example of valid RoleBinding that gives to the user 'torch' the uthorization required by TORCH-V2 to correctly operate. As always we can apply the file using kubectl:  
`kubectl apply -f role-binding-torch-admin.yaml`  

## Create the credentials to authenticate TORCH-V2
To let TORCH authenticate in each Cluster as the user "torch" we first need to generate a private key for the user "torch":  
`openssl genrsa -out torch-admin.key 2048`  

We then create a CSR stating that the generated private key is owned by the user "torch":  
`openssl req -new -key torch-admin.key -out torch-admin.csr -subj "/CN=torch-admin"`  
The CSR must be presented to the kubernetes CA so that we can receive a CA signed certificate to present to Kubernetes to authenticate as "torch". To do so we create a kubernetes object called CertificateSigningRequest and apply it to the cluster. The file "csr-torch.yaml" is an example of valid CertificateSigningRequest. To use it you need to change the value assigned to the field "request" with a base64 encoded version of the csr we generated in the previous step. You can obtain the base64 encoded version of the csr using the following command :  
`cat torch-admin.csr | base64 | tr -d "\n"`  
Copy the resulting string and paste it as value for the "request" field, then apply the file to each cluster:  
`kubectl apply -f csr-torch-admin.yaml`  

Approve the CertificateSigningRequest using kubectl and retrieve the signed certificate:  
`kubectl certificate approve torch-admin`  
`kubectl get csr torch-admin -o jsonpath='{.status.certificate}'| base64 -d > torch-admin-signed.crt`  


## Download TORCH-V2

Download TORCH-V2 repository and set the root folder as your working directory.
```bash
cd an/empty/folder/
git clone https://gitlab.com/MMw_Unibo/escalation/torch_v2.git
cd torch_v2
export BASE_DIR=$(pwd)
cd examples/two-clusters
export EXAMPLE_DIR=$(pwd)
```
## Building and Starting the Docker images for the main components of TORCH-v2:
Currently the docker images of the varius components of TORCH-v2 are not availble in a public repository like DockerHub, so you need to build them using the Dockerfiles available on the TORCH-v2 repository.  
### Build and Start the Dashboard
```bash
cd $BASE_DIR/dashboard
docker build -t edrudi97/torch-dashboard:v0.1.0 . #you can choose a different name
cd $EXAMPLE_DIR/docker
#before the next step, update the docker compose file accordingly to the name you chose for the image
docker compose -f docker-compose-torch-dashboard-solver.yaml up -d
```
### Build and Start the Flowable Engine
```bash
cd $BASE_DIR/model/flowable
docker build -t edrudi97/custom-flowable-rest:v0.1.0 . #you can choose a different name
cd $EXAMPLE_DIR/docker
#before the next step, update the docker compose file accordingly to the name you chose for the image
docker compose -f docker-compose-flowable-rest-solver.yaml up -d
#WAIT for flowable to be ready to serve http requests, then execute the command below
curl -u rest-admin:test -X POST http://localhost:8080/flowable-rest/service/repository/deployments \
    -H "Content-Type: multipart/form-data" -F "archive=@bpmn_plans_v2_3_8.zip;type=application/zip"
```
### Configure, Build and Start the Service Connector Layer

Before we build and start the Service Connector Layer we need to configure it. Currently, the TORCH-v2 Service Connectors can only be configured before starting them so we need to configure the Service Layer before building an image for it. To configure the Service Layer with the credentials required to access the Kubernetes Cluster you need to copy:
  1. the generated private key (torch.key)
  2. the two certificate signed by the kubernetes clusters
  3. and the CA certificates of the kubernetes Clusters
inside the folder $BASE_DIR/service-layer/service-binding/instantiate-k8s-cluster-manual/src/main/resources.

NOTE: you can use an external CA to authenticate the TORCH user and the Cluster. For semplicity we use the CA of the Kubernetes Cluster.
 
Now we can build and start the image
```bash
cd $BASE_DIR/service-layer
docker build -t edrudi97/torchservices:v0.1.0 . #you can choose a different name
cd $EXAMPLE_DIR/docker
#before the next step, update the docker compose file accordingly to the name you chose for the image
docker compose -f docker-compose-torchservices-solver.yaml up -d 
```
  
## Deploy pipeline1 with TORCH-v2
1. go to TORCH-V2 webpage (http://localhost:8005/)  
2. register an account for yourself  
3. login and you will reach the home webpage  
4. Upload the csar file by clicking on "Upload TOSCA Template". ATTENTION: The Template expect that the local OSRM server is reachable at host "osrm.default" and port "5000", if your local OSRM server has a different configuration you have to change the input OSRM_HOST and OSRM_PORT of the pipeline orchestrator node.
5. click on "validate", then on "create". You will return automatically on the home page which now should show the created template  
7. click on "Deploy scheme" and the deploy will start

## Monitoring the deployment with TORCH-v2
1. go to 'home' ((http://localhost:8005/home))
2. click on the active template whose deployment you want to monitor. This will open another webpage where you can see the status of your application

In the upper-left part of the monitoring page you can see the graph representing your application. In the upper-right corner you can instead see the status of each components of your application. If a component has status "STARTED" its deployment was successful, if its in the "ERROR" state an error has occured. Other states like INITIAL, STARTING, ... mean that the deployment is still ongoing. If you scroll down on the monitoring pages you will see some additional information abount the component whose deployment has terminated (successfully or with error). The page is refreshed every 30 seconds so it may take some times before you can see these additional info.

## Use the deployed pipeline application
1. Go to the monitoring page (see previous section) and wait for all the components to be in the "STARTED" state.
2. Check in the lower part of the page which port and domain name is the component pipeline_orchestrator using. These info can be retrieved by looking at the "app-endpoint" attributes of the pipeline_orchestrator component, under the name respectively of "public_port" and "public_domain_name"
3. (optional) apply on the federation namespace ("<your-template-name>-federation") the tester.yaml file to receive the result of the optimization pipeline. You can also interrogate each microservice to retrieve partial results and from the last microservice (compactor) you can retrieve the optimized route without having to deploy the tester.
4. Use these info to make an http post to the pipeline orchestrator to optimize route using the RVRPTW Solver and Route Compactor. For example, you can use the following command: `curl -X POST http://k8scluster-endpoint:31307/api/v1/use-chain -H "Content-Type: application/json"  -d "@input-test.json"`. Remember to change the port and hostname with the app-endpoint.public_port and app-endpoint.public_domain_name info.

N.B., before trying again with another example be sure to remove from your cluster the resources created with this example. There should be three namespace that were created to deploy this application: two in the entrypoint cluster (called <your-template-name> and "<your-template-name>-federation") and one in the other cluster (called <your-template-name>). You can delete all the resources using `liqoctl unoffload namespace "<your-template-name>-federation"` and `kubectl delete namespace <namespace_name>` on each cluster.




