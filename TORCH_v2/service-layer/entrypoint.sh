#!/bin/bash

# set up a folder that can be used by connector to install additional content or bins
mkdir /usr/app/utils
mkdir /usr/app/utils/bin

# add the utils/bin folder to PATH
export PATH=$PATH:/usr/app/utils/bin

# NB: for better security when running third party connectors it would be better to create a new USER with less priviledge and 
# use this to run the java connectors. For now we stick to Root user.

#copy all jars into 'jars' folder for convenience
cd /usr/app/service-binding
declare -a ARR=("instantiate-du-kubernetes"
                "instantiate-k8s-cluster-manual"
                "add-storage-kubernetes"
                "instantiate-volume-kubernetes"
                "instantiate-liqo-federation"
                "instantiate-du-liqo"
                )

for SC in "${ARR[@]}"
do
   cd $SC
   OUTPUT_FILENAME="${SC}-logs.txt"
   java -Dvertx.options.blockedThreadCheckInterval=20000000000 -jar target/$SC-0.0.1-SNAPSHOT-fat.jar > $OUTPUT_FILENAME 2>&1 &
   cd ..
done
sleep 15

#finally, start the service broaker
cd service-broker
java -Dvertx.options.blockedThreadCheckInterval=20000000000 -jar target/service-broker-0.0.1-SNAPSHOT-fat.jar
