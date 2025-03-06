# TORCH_v2

The original TORCH project is available at: https://github.com/unict-cclab/TORCH/tree/master

TORCH_v2 is a TOSCA Orchestrator based on the TORCH project. The purpose of TORCH_v2 is to simplify the deployment of distributed applications across multiple and different cloud-resources (i.e., over a Hybrid Cloud infrastructure). 

 
## Short Overview

@TODO brief description of the TORCH Architecture

Follow the links below to find a more detailed documentation for each key part of TORCH_v2:
- [TORCH_v2 TOSCA types](doc/TOSCA-torch-model/torch-model-index.md)
- [TOSCA Processor]() @TODO
- [Dashboard]() @TODO
- [BPMN workflow](doc/bpmn-plans/BPMN-doc-index.md) 
- [Service Layer]() @TODO 

## Installation

Download TORCH-V2 repository and set the root folder as your working directory.
```bash
cd an/empty/folder/
git clone https://gitlab.com/MMw_Unibo/escalation/torch_v2.git
cd torch_v2
export BASE_DIR=$(pwd)
cd examples/my-solver-and-georef
export EXAMPLE_DIR=$(pwd)
```
Currently, to use install TORCH_v2 you are required to perform specific configuration operations and build some docker images using the various Dockerfiles present in the repository. Refers to the examples in the *Usage section* for more information on how to correctly configure and install TORCH_v2 for your specific use case.

## Usage
List of available examples to learn how to use TORCH_v2:

- [Single Cluster examples]()  
- [Multi-cluster examples](examples/RVRPTW-and-compactor/pipeline.md): deploy a multi-cluster system using TORCH_v2

## Known limitation and difference wrt the original TORCH project
TORCH_v2 is a work in progress and follow most the specification written in the [TOSCA Simple Profile 1.X](https://docs.oasis-open.org/tosca/TOSCA-Simple-Profile-YAML/v1.3/TOSCA-Simple-Profile-YAML-v1.3.html). TORCH_v2 support the TOSCA Simple Profile v1.3 with some limitation:
- The Standard node and relationship types defined in the TOSCA Simple Profile are not supported, instead TORCH_v2 offers a set of base node and relationship type you can use to describe your application
- Many-to-One and One-to-Many relationships are not yet supported. Currently, torch support only one-to-one relationship. This means that while a capability can be used by multiple dependant nodes, a requirement can be associated with one and only one capability.
- TORCH_v2 support the TOSCA functions: get_property() and get_inputs(). Currently, TORCH_v2 support the get_attribute() function only when used in Interface inputs and linked to attributes related to a capability.
- Reflected Attributes are fixed and defined in the standard TOSCA types available. Properties are not automatically associated to an implicit attribute so every attribute that is actually reflected at runtime is explicitily defined in each node, capability and relationship type available. Also, currently attributes are computed only after the Nodes are started but not updated periodically. In the future we will support the dynamic update of these attributes and of all the Nodes that use them.
- custom lifecycle management logic is not supported so no mechanism to inject such a logic is available in TORCH_v2 except for the possibility of setting a docker image as an implementation for a specific application node.

TORCH_v2 is shipped with custom TOSCA types that are supported and can be used in your TOSCA Template. TORCH_v2 does not support the standard types defined in the TOSCA Simple Profile nor will it support them is the future. This is one of the main difference between TORCH_v2 and the original TORCH which instead tries to support some of the Simple Profile types. We belive that the Simple Profile types are too general to capture some of the information required to successfully deploy and manage distributed abblications on multiple Cloud infrastructures, and because of this we decided to make our own custom TOSCA types (still based on the root types proposed in the Simple Profile). Our TOSCA model provides TOSCA type definitions for the most common concept used in Cloud-native scenarios, like the concept of Cluster, without restincting it to specific cloud tecnologies (like Kubernetes). This choice let us have all the information we need to deploy and manage cloud-native applications while also remaining general enough to be usable in different scenarios using different cloud tecnologies. It is also worth noting that starting from [TOSCA v2.0](https://docs.oasis-open.org/tosca/TOSCA/v2.0/TOSCA-v2.0.html) (which is the successor of TOSCA Simple profile) the Technical Committee decided to remove the standard Simple Profile types because it was indeed considered too restrictive. Therefore, starting from TOSCA v2.0 it is expected that every domain use-case will have their own custom types and TOSCA Orchestrator are not required to have a built-in implementation for the Simple Profile types to be TOSCA-compliant. The old Simple Profile types, plus some other community proposed standard types, became simply "Profiles" that can be supported by TOSCA Orchestrators, but this support is no more required to be compliant with the TOSCA standard.

With respect to the original TORCH project, TORCH_v2 also add:
- support for get_input(), get_property() and get_attribute() TOSCA functions with the limitation reported above
- support for the deployment over a manually created Kubernetes Cluster
- support for the reflections of TOSCA attributes associated with TORCH custom types. These attributes can be monitored via the Dashboard and can be used inside the TOSCA template via the get_attribute() function to allow at-runtime configuration of the various Node of the Template.
- implementation for the custom types introduced by TORCH_v2. The definitions of TORCH_v2 custom types are available [here](dashboard/dashboard/public/json4tosca-parser/toscaparser/extensions/torch/TOSCA_torch_definition_1_0_1.yaml).
