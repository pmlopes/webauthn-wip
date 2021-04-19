FROM maven:3.6.1-jdk-11-slim as maven
WORKDIR /vertx
COPY src src
COPY pom.xml pom.xml
RUN mvn package -q

FROM adoptopenjdk:16-jre-hotspot
WORKDIR /vertx
ARG VERSION=4.0.3
ARG PORT=8443
COPY --from=maven /vertx/target/webauthn-${VERSION}.jar /vertx/webauthn-vertx-server.jar

EXPOSE $PORT

CMD ["java", "-jar", "webauthn-vertx-server.jar", "-conf", "config.json"]
