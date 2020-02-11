# WebAuthN Vert.x Demo

This is a small PoC for WebAuthN vert.x application.

All users are ephemeral as the data is stored in memory in the class `InMemoryStore`.

# Development

In order to run this app you need `vertx-auth` and `vertx-web` from the master branch.

Download the `vertx-auth/master` branch and build it locally:
```
curl https://github.com/vert-x3/vertx-auth/archive/master.zip
unzip master.zip
cd master
mvn -Dmaven.test.skip=true install
```

Download the `vertx-web/master` branch and build it locally:
```
curl https://github.com/vert-x3/vertx-web/archive/master.zip
unzip master.zip
cd master
mvn -Dmaven.test.skip=true install
```

Once these are in your local maven cache you don't need to do the previous steps (unless the branches change).

To test on a local machine there is no need to use SSL, however to connect to the app from a different machine/device the browser credentials API requires an SSL certificate.

To create a self signed key for your IP address do the following:

```
# replace the CN with your own IP address (other than localhost) with suffix .xip.io
keytool -genkeypair -alias rsakey -keyalg rsa -storepass passphrase -keystore mytestkeys.jks -storetype JKS -dname "CN=192.168.178.74.xip.io,O=Vert.x Development"
# convert to PKCS#12 format for compatibility reasons
keytool -importkeystore -srckeystore mytestkeys.jks -destkeystore mytestkeys.jks -deststoretype pkcs12
# your new ssl certificate is on the file `mytestkeys.jks`
```

Update the `MainVerticle` to use this new certificate store.

Remember this is a self signed certificate so it will cause warnings all over, if you want to test it fully you need a verified certificate perhaps using:
https://letsencrypt.org .
