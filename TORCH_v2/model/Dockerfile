#
# Build stage
#
#FROM eclipse-temurin:8-jdk-focal AS build
FROM maven:3.9.6-eclipse-temurin-8-focal AS build
ENV HOME=/usr/app
RUN mkdir -p $HOME
WORKDIR $HOME
COPY service-binding-elisa $HOME/service-binding

#MAVEN_CONFIG=/root/.m2  from BaseImage!! therefore, the maven cache is there even if HOME changes
# since mave is installed, we use the mvn command
# if we started from a BaseImage without maven installed we could use the mvnw (maven wrapper) command
RUN --mount=type=cache,target=/root/.m2 mvn -f $HOME/service-binding/pom.xml clean package

#COPY copy_jars.sh $HOME/copy_jars.sh
#RUN chmod +x $HOME/copy_jars.sh
#SHELL ["/bin/bash"]
#RUN ./copy_jars.sh
#RUN ["/bin/bash", "-c", "$HOME/copy_jars.sh"]  # RUN not working with bash script...???
#RUN ["", ""]/bin/bash -c './copy_jars.sh'
#CMD ["sleep", "infinity"]


#
# Package stage (maven and jdk are not needed anymore)
#
FROM eclipse-temurin:8-jre-focal 
#ENV JAR_DIR=/usr/app/service-binding/jars
ENV HOME=/usr/app
RUN mkdir -p $HOME
WORKDIR $HOME
COPY entrypoint.sh $HOME/entrypoint.sh
RUN chmod +x $HOME/entrypoint.sh
COPY --from=build /usr/app/service-binding  service-binding

#EXPOSE 8080
#ENTRYPOINT java -jar /app/runner.jar
CMD ["/bin/bash", "-c", "$HOME/entrypoint.sh"]



#FROM ubuntu:22.04

#RUN DEBIAN_FRONTEND=noninteractive \
#  apt-get update \
#  && apt-get install -y python3 \
#  && rm -rf /var/lib/apt/lists/*

#RUN useradd -ms /bin/bash torchrunner

#WORKDIR /home/torchrunner/
#RUN mkdir torch 
#WORKDIR /home/torchrunner/torch

#COPY --chown=torchrunner test.txt test.txt 

#USER torchrunner

#CMD ["sleep", "infinity"]