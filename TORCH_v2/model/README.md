## JAVA MODELS for Torchv2

Until we merge both model into a single one (@TODO) we'll have two different jar with different java classes to represents the main concepts used in TORCH.
These classes are a work in progress.

### Folder Structure
```
	model->flowable->unict-activiti-extensions-v0_1: contains classes used in the BPMN Plans (both model classes and delegates with logic used inside some of the BPMN tasks). 
					 								 The resulting jar is copied inside the classpath used by Flowable Engine.
	model->java-connectors->torch-java-model: contains classes used in the Service Layer. The resulting jar is copied inside the lib folder of the Service Layer folder.
```

### How to compile the model (without installing a jdk on you local pc) with docker

a) start a docker container with jdk and maven already installed  
		`docker compose -f docker-compose-compilation-env.yaml up -d`

b) copy the project folder inside the container  
		`docker cp ./torch-java-model/. maven-eclipse-temurin:/torch-java-model`

c) exec into it and compile the java code  
		`docker exec -it maven-eclipse-temurin /bin/bash`
		`mvn -f torch-java-model/pom.xml clean package`

d) save locally the resulted jar file  
		`docker cp maven-eclipse-temurin:/torch-java-model/target/java-model-0.1.0-SNAPSHOT.jar .`

e) kill the docker container  
		`docker compose -f docker-compose-compilation-env.yaml down -t2`

### How to inspect the methods of a class inside a jar
javap -classpath <myjar.jar> <my-pkg>.<MyClass>