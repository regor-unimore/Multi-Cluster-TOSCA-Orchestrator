# @TODO - KinD guide

## Prerequisites
 A computer powerful enough to run a KinD Cluster and the docker containers required by TORCH-V2 (an old laptop (~2016) with 12GB of RAM was able to run it, so the chances are that you should be able to run it too on your laptop/pc :) )
- [Docker](https://docs.docker.com/engine/install/) installed and, if you are using a Linux distro, make sure to set docker to be runnable [without root](https://docs.docker.com/engine/install/linux-postinstall/#manage-docker-as-a-non-root-user)
- [kind](https://kind.sigs.k8s.io/docs/user/quick-start/#installation) installed
- [kubectl](https://kubernetes.io/docs/tasks/tools/#kubectl) installed


NOTE: The commands reported in this guide presume you are using bash to run them. For other type of shell the syntax may differ.  

### Create a Cluster with KinD

`cd kind-cluster`

You can create a KinD cluster named "bologna" (the name is not relevant, you can use another name if you want) using the following kind command:   
`kind create cluster --name bologna --kubeconfig kubeconfig_bologna --config kind_cluster.yaml`  

With `--config` we can specify a yaml file from which `kind` will read the configuration to adopt for creating the cluster. In our simple case , we specifyed that we want to use kubernetes 1.28 and we want our cluster to be composed by two nodes: one worker and one control plane. Please note that your kubectl version and the kubernetes version must be compatible as per [kubernetes version skew policy](https://kubernetes.io/releases/version-skew-policy/), which means your kubectl and kubernetes version must differ by one minor version at most.  
You can also use `--kubeconfig` to specify where kind should save the kubeconfig which can be useful when we want to handle more than one cluster (which is not our case). By default, KinD store the kubeconfig inside the .kube/config file in the user home.  

You can check if the cluster is up and running with `kubectl`:  
`kubectl --kubeconfig kubeconfig_bologna get nodes`

You should also enable rbac authorization. KinD cluster should already enable RBAC by default. You can check if RBAC is enabled with the following command:
`kubectl api-versions --kubeconfig kubeconfig_bologna | grep rbac`

### Retrieve Kubernetes CA certificate

To authenticate TORCH-V2 as "torch" in the kubernetes cluster we need to pass to TORCH-V2 the generated private key (torch.key), the certificate signed by the kubernetes cluster (torch-signed.crt) and finally we need the certificate of the kubernetes CA (required by TORCH-V2 to authenticate the kubernetes cluster during the authentication process since both parties must be able to authenticate the other). We can retrieve the certificate of the KinD kubernetes CA using the following command:  
`docker cp bologna-control-plane:/etc/kubernetes/pki/ca.crt .`  

### Retrieve Kubernetes API-server endpoint

We also need to pass the url of the API-server endpoint to TORCH-v2 so that it can execute command against the cluster api-server. 

We suggest to run Torch docker images in the same network as your Kind Cluster(s) so you can the internal cluster endpoint. To retrieve the endpoint exposed internally by the KinD cluster use the following command:  
`kind get kubeconfig --internal --name bologna`  
and copyng the url specified under the "server" field. It should be something like "https://bologna-control-plane:6443"  

If you instead want to run Torch in a different docker network, you have to use the external endpoint which you can find specified inside the kubeconfig file. It should be something like "https://127.0.0.1:45645". To use this endpoint you need to change "127.0.0.1" with a name or ip that can be eventually resolved to the localhost of the docker host (some DNATs may be required) or you can use the internal endpoint and adjust the firewall rule added by Docker to let the traffic reach the Control Plane of your KinD cluster despite the different network. 

## ERROR: too many file open

This may be caused by running out of inotify resources. Resource limits are defined by fs.inotify.max_user_watches and fs.inotify.max_user_instances system variables. For example, in Ubuntu these default to 8192 and 128 respectively, which is not enough to create a cluster with many nodes. To increase these limits temporarily run the following commands on the host:
`sudo sysctl fs.inotify.max_user_watches=524288`
`sudo sysctl fs.inotify.max_user_instances=512`

## ERROR: KinD Pods cannot access internet
This is usually a DNS problem.

If your domain name resolver is not setted properly inside the KinD nodes you may incurr in problem with accessing external service using their domain name. You can easily check if this is the case by executing inside one Pod a ping towards www.google.com and 8.8.8.8, if the ip address work but the domain name not then is it indeed a problem with domain resolution. You can try destroying your cluster and recreating it using as kind config file the file "kind_cluster_without_search_domain.yaml". It sets a custom resolv.conf to force KinD to use as the default DNS your docker host (you may need to change the file resolv.conf so that the nameserver point to the ip address that your docker host uses in the network used by the KinD Cluster).

If both 8.8.8.8 and www.google.com don't work then it could be a problem related to some firewall rules.