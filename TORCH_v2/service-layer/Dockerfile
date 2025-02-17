#
# Build stage
#
#FROM eclipse-temurin:8-jdk-focal AS build
FROM maven:3.9.6-eclipse-temurin-8-focal AS build
ENV HOME=/usr/app
RUN mkdir -p $HOME
WORKDIR $HOME
COPY service-binding $HOME/service-binding
#add custom libs to a local maven repository
COPY prepare_maven_repo.sh $HOME/prepare_maven_repo.sh
RUN chmod +x $HOME/prepare_maven_repo.sh
#RUN ["./prepare_maven_repo.sh"]
#MAVEN_CONFIG=/root/.m2  from BaseImage!! therefore, the maven cache is there even if HOME changes
# since mave is installed, we use the mvn command
# if we started from a BaseImage without maven installed we could use the mvnw (maven wrapper) command
RUN --mount=type=cache,target=/root/.m2 bash ./prepare_maven_repo.sh


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

CMD ["/bin/bash", "-c", "$HOME/entrypoint.sh"]

