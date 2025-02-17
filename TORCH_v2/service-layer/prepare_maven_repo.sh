#!/bin/bash

#we prepare a local maven repo with the jar contained in the lib folder
#  this is one of the possible solution to make sure that maven package
#  these jars with the final jars of the project, usefule when we have
#  external dependencies not available online on maven official repos
mvn install:install-file -Dfile=service-binding/lib/java-model-0.1.1-SNAPSHOT.jar \
                         -DgroupId=torch \
                         -DartifactId=java-model \
                         -Dversion=0.1.1 \
                         -Dpackaging=jar #\
#                         -DlocalRepositoryPath=service-binding/local_repo

mvn -f $HOME/service-binding/pom.xml clean package
