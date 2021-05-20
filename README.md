# WebAuthN Vert.x Demo

This is a small PoC for WebAuthN vert.x application.

All users are ephemeral as the data is stored in memory in the class `InMemoryStore`.

# Development

To test on a local machine there is no need to use SSL, however to connect to the app from a different machine/device
the browser credentials API requires an SSL certificate.

To create a self-signed key for your IP address do the following:

```bash
# replace the CN with your own IP address (other than localhost) with suffix .nip.io
keytool -genkeypair -alias rsakey -keyalg rsa -storepass passphrase -keystore mytestkeys.jks -storetype JKS -dname "CN=192.168.178.74.nip.io,O=Vert.x Development"
# convert to PKCS#12 format for compatibility reasons
keytool -importkeystore -srckeystore mytestkeys.jks -destkeystore mytestkeys.jks -deststoretype pkcs12
# your new ssl certificate is on the file `mytestkeys.jks`
```

# Docker usage

A docker image can be built using:

```bash
docker \
  build \
  -t webauthn:4.0.3 \
  --build-arg IP=192.168.178.210 \
  --build-arg CERTSTORE_SECRET=secret \
  .
```

The container will create a self-signed certificate given the builder's IP address.

In order to run the image with a custom config do:

```bash
docker run \
  --rm \
  --net host \
  -v $(pwd)/config.json:/vertx/config.json \
  webauthn:4.0.3
```

Optionally custom metadata statements can be mounted into the container, to avoid calling the MDS server:

```bash
docker run \
  --rm \
  --net host \
  -v $(pwd)/config.json:/vertx/config.json \
-v $(pwd)/metadataStatements:/vertx/metadataStatements \
  webauthn:4.0.3
```


## The base config

See the [config.json](config.json)

Remember this is a self-signed certificate, so it will cause warnings all over, if you want to test it fully you need a
verified certificate perhaps using: https://letsencrypt.org .
