FROM maven:3.6.1-jdk-11-slim as maven
WORKDIR /vertx
COPY src src
COPY pom.xml pom.xml
RUN mvn package -q

FROM adoptopenjdk:16-jre-hotspot
WORKDIR /vertx
ARG VERSION=4.0.3
ARG IP
ARG CERTSTORE_SECRET

COPY --from=maven /vertx/target/webauthn-${VERSION}.jar /vertx/webauthn-vertx-server.jar

EXPOSE $PORT

RUN echo ${CERTSTORE_SECRET} | keytool \
  -genkeypair \
  -alias rsakey \
  -keyalg rsa \
  -storepass ${CERTSTORE_SECRET} \
  -keystore cert-store.jks \
  -storetype JKS \
  -dname "CN=${IP}.nip.io,O=Vert.x Development"

RUN echo ${CERTSTORE_SECRET} |  keytool \
  -importkeystore \
  -srckeystore cert-store.jks \
  -destkeystore cert-store.jks \
  -deststoretype pkcs12

ENV IP=${IP}
ENV CERTSTORE_SECRET=${CERTSTORE_SECRET}

CMD ["java", "-jar", "webauthn-vertx-server.jar", "-conf", "config.json"]
