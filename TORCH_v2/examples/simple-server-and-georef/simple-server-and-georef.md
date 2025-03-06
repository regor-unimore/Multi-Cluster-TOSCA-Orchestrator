# EXAMPLE: Simple Server and Georef

This guide will show you how you can use TORCH-V2 to deploy a simple application consisting of two docker-containerized application on a Kubernetes Cluster. This guide will show you how you can use TORCH-V2 to deploy a simple application consisting of two docker-containerized application on a Kubernetes Cluster. The TOSCA representation of this example application is contained in the CSAR file simple-server-and-georef.zip. This distributed application consists of two simple microservices, packaged as Docker images: Georef and SimpleServer. The Georef service, developed by UniMORE, is a Python-based georeferencing module that provides an HTTP endpoint for obtaining the geographical coordinates of two locations based on their addresses. SimpleServer is a simple Python-based HTTP server that makes the Georef service accessible to external users by forwarding incoming HTTP requests to it. For the application to function, both Georef and SimpleServer must be deployed on the available Kubernetes cluster, and SimpleServer needs to be configured to connect to and utilize the Georef module. In this guide, you will see how Torch automatically fulfills both these requirements with no manual intervention beyond the initial minimal configuration.  

## Prerequisites
- To follow this guide you need to create a Kubernetes Cluster (1.28+) if you don't already have one available. You can create a local cluster using KinD on your pc or using kubeadm on two or more VMs or physical nodes. Check our [KinD guide](../../doc/cluster-guide/kind-guide.md) to learn how you can create a local Kubernetes cluster using docker. A guide for baremetal Kubernetes with kubeadm will be available in the future.
- [kubectl](https://kubernetes.io/docs/tasks/tools/#kubectl) installed and setted up to reach your cluster
- [Docker](https://docs.docker.com/engine/install/) installed and, if you are using a Linux distro, make sure to set docker to be runnable [without root](https://docs.docker.com/engine/install/linux-postinstall/#manage-docker-as-a-non-root-user)

NOTE: The commands reported in this guide presume you are using bash to run them. For other type of shell the syntax may differ.  

## Create a RBAC Role for TORCH-V2

The current version of TORCH-V2 can orchestrate the creation, update and deletion of "pods","services" and "deployments" on the namespace <your-template-name>. You need to give to TORCH the authorization to act on these kubernetes resources on your cluster on the default namespace. To do so we can use the RBAC authorization method that is supported natively by kubernetes.  

First of all, enable (if it's not already enabled) RBAC on your cluster. KinD already enable it by default so if you are testing TORCH with it no further operation are required.  
You can check if RBAC is enabled on a general Kubernetes cluster by listing the api-server extension enabled and searching for `rbac.authorization.k8s.io`.  
I.e;  
`kubectl api-versions | grep "rbac.authorization.k8s.io/v1"`  

Then we need to crete a Role with the authorization for TORCH-V2. The file role-torch.yaml in the folder *examples/simple-server-and-georef/resources* already contains the correct authorization, we can apply them to the cluster:  
`kubectl apply -f role-torch-admin.yaml`  

We now associate the Role with the authorization to a username that from now on will represents TORCH-V2 as a user. The file role-binding-torch.yaml show an example of valid RoleBinding that gives to the user 'torch' the uthorization required by TORCH-V2 to correctly operate. As always we can apply the file using kubectl:  
`kubectl apply -f role-binding-torch-admin.yaml`  

## Create the credentials to authenticate TORCH-V2
To let TORCH authenticate in the Cluster as the user "torch" we first need to generate a private key for the user "torch":  
`openssl genrsa -out torch-admin.key 2048`  

We then create a CSR stating that the generated private key is owned by the user "torch":  
`openssl req -new -key torch-admin.key -out torch-admin.csr -subj "/CN=torch-admin"`  
The CSR must be presented to the kubernetes CA so that we can receive a CA signed certificate to present to Kubernetes to authenticate as "torch". To do so we create a kubernetes object called CertificateSigningRequest and apply it to the cluster. The file "csr-torch-admin.yaml" is an example of valid CertificateSigningRequest. To use it you need to change the value assigned to the field "request" with a base64 encoded version of the csr we generated in the previous step. You can obtain the base64 encoded version of the csr using the following command :  
`cat torch-admin.csr | base64 | tr -d "\n"`  
Copy the resulting string and paste it as value for the "request" field, then apply the file to the cluster:  
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
cd examples/simple-server-and-georef
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
  1. the generated private key (torch-admin.key)
  2. the certificate signed by the kubernetes cluster (torch-admin-signed.crt)
  3. and the CA certificate of the kubernetes Cluster  
  
inside the folder $BASE_DIR/service-layer/service-binding/instantiate-k8s-cluster-manual/src/main/resources.

NOTE: you can use an external CA to authenticate the TORCH user and the Cluster. For semplicity we use the CA of the Kubernetes Cluster.

```bash
# i.e.,
cp ./{torch-admin.key,ca.crt,torch-signed.crt} $BASE_DIR/service-binding/instantiate-k8s-cluster-manual/src/main/resources/
```  
Now we can build and start the image
```bash
cd $BASE_DIR/service-layer
docker build -t edrudi97/torchservices:v0.1.0 . #you can choose a different name
cd $EXAMPLE_DIR/docker
#before the next step, update the docker compose file accordingly to the name you chose for the image
docker compose -f docker-compose-torchservices-solver.yaml up -d 
```
  
## Deploy the example with TORCH-v2

Before starting be sure to update the properties of the Cluster node in the "simple-server-and-georef.zip" CSAR to match the names of the files you used to configure the Service Layer and update the api-endpoint propertiy with your Kubernetes Cluster API endpoint.  

1. go to TORCH-V2 webpage (http://localhost:8005/)  
2. register an account for yourself  
3. login and you will reach the home webpage  
4. click on "Upload TOSCA Template", give a name to the template and upload the file "simple-server-and-georef.zip" you can find in the $EXAMPLE_DIR folder  
5. click on "validate", then on "create". You will return automatically on the home page which now should show the created template  
6. click on the name of you template, then select "Manual" as Cloud Provider and "Kubernetes" as Cloud Platform. You can ignore the other configurations for now.
7. click on "Deploy scheme" and the deploy will start

## Monitoring the deployment with TORCH-v2
1. go to 'home' ((http://localhost:8005/home))
2. click on the active template whose deployment you want to monitor. This will open another webpage where you can see the status of your application

In the upper-left part of the monitoring page you can see the graph representing your application. In the upper-right corner you can instead see the status of each components of your application. If a component has status "STARTED" its deployment was successful, if its in the "ERROR" state an error has occured. Other states like INITIAL, STARTING, ... mean that the deployment is still ongoing. If you scroll down on the monitoring pages you will see some additional information abount the component whose deployment has terminated (successfully or with error). The page is refreshed every 30 seconds so it may take some times before you can see these additional info.

## Use the deployed application
1. Go to the monitoring page (see previous section) and wait for all the components to be in the "STARTED" state.
2. Check in the lower part of the page which port and domain name is the component simple-server using. These info can be retrieved by looking at the "app-endpoint" attributes of the simple-server component, under the name respectively of "public_port" and "public_domain_name"
3. Use these info to make an http post to simple-server to georeference some coordinates. For example, you can use the following command: `curl -X POST http://k8scluster-endpoint:30186/georef -H "Content-Type: application/json"  -d "@input-test.json"`. Remember to change the port and hostname with the app-endpoint.public_port and app-endpoint.public_domain_name info.

N.B., before trying again with another example be sure to remove from your cluster the resources created with this example. There should be two Deployments and two Services (inside the 'default' namespace) that need to be deleted in this case. You can delete resources using `kubectl delete <resource>`.