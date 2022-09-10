ARG UBI=ghcr.io/graalvm/native-image:latest
ARG MVN_OPTIONS=-f /opt/quarkus-src/pom.xml -B -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn 

FROM ${UBI} AS build
COPY . /opt/quarkus-src
WORKDIR /opt/quarkus-src
RUN ./mvnw $MVN_OPTIONS org.apache.maven.plugins:maven-dependency-plugin:3.3.0:go-offline
RUN ./mvnw $MVN_OPTIONS package

FROM ${UBI} AS runtime
EXPOSE 8080
COPY --from=build /opt/quarkus-src/target/ /opt/quarkus-app/
ENTRYPOINT ["java","-jar","/opt/quarkus-app/quarkus-app/quarkus-run.jar"]

# Cheat Sheet
# docker build --no-cache --progress=plain -t caravanacloud/fractal:latest . 
# docker run -ti --rm -p8080:8080 caravanacloud/fractal:latest
# docker login 
# docker push caravanacloud/fractal:latest