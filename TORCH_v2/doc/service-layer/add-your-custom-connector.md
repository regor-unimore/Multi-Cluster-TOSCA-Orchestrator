# How to add your custom Connectors to the Service Layer

I Connettori sono componenti che contengono la logica che traduce le azioni generali di orchestrazione di TORCH-V2 in azioni specifiche per determinate tecnologie che si vogliono usare. Per ogni tecnologia che si vuole supportare è necessario creare uno o più connettori. (Idealmente sarebbe bello dover scrivere solo 1 connettore per tecnologia @TODO -> diventa possibile se forziamo contratti specifici tra i diversi tipi di Nodi secondo tutte e sole le relazioni base che supportiamo -> ossia, sono le relazioni che se hanno interfacce poco definite ci costringono a costruire più di un connettore, d'altronde è a causa delle relazioni che nodi diversi basati potenzialmente su tecnolgie diverse devono comunicare)

## 1. Attach your connectors to the Service Broker

### Register your Connectors in the discovery file
The Service Broker does not implement yet a dynamic service discovery mechanism, it instead read from a file the list of connectors that are available at start time. To make your connector usable by TORCH-v2 you have to add manually the info of your connector to the [service discovery file](../../service-layer/service-binding/service-broker/src/main/resources/services.json)

Be sure to not use a port that is already used for your connector if you run the connector in the same container as the standard TORCH-v2 connectors.
<table>
  <tr>
    <th>TORCH-v2 common connector</th>
    <th>Used Port</th>
  </tr>
  <tr>
    <td>Service Broker</td>
    <td>9000</td>
  </tr>
  <tr>
    <td>Kubernetes DU</td>
    <td>9013</td>
  </tr>
  <tr>
    <td>Manual Kubernetes</td>
    <td>9019</td>
  </tr>
  <tr>
    <td>Kubernetes VolumeProvider</td>
    <td>9020</td>
  </tr>
  <tr>
    <td>Kubernetes Volume</td>
    <td>9021</td>
  </tr>
  <tr>
    <td>Liqo Federation</td>
    <td>9022</td>
  </tr>
    <tr>
    <td>Liqo DU</td>
    <td>9023</td>
  </tr>
</table>


### Name your Connectors following TORCH-v2 Service Layer convention

At start time, the Service Broker will read this file and try to connecto to each connector via the ESB using the name: "PREFIX-NAME" where 'NAME' is the name written in the homonym field in the discovery file, while 'PREFIX' is currently fixed and depends on the type of node you are implementing.

<table>
  <tr>
    <th>TOSCA Node Type</th>
    <th>Expected Prefix</th>
  </tr>
  <tr>
    <td>ApplicationService</td>
    <td>instantiate-du-</td>
  </tr>
  <tr>
    <td>Cluster</td>
    <td>instantiate-cluster-</td>
  </tr>
  <tr>
    <td>VolumeProvider</td>
    <td>add-storage-</td>
  </tr>
  <tr>
    <td>Volume</td>
    <td>instantiate-volume-</td>
  </tr>
  <tr>
    <td>Federation</td>
    <td>instantiate-federation-</td>
  </tr>
</table>

The actual name of your connector is not important, what is important is that the name used to register in the ESB follow the above convention.

### Start your Connectors before the Service Broker

Aggiorna lo script [entrypoint](../../service-layer/entrypoint.sh) del Service Layer e ricompila l'immagine docker.

