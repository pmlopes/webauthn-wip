package com.example.webauthn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.FileSystemException;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.auth.webauthn.WebAuthn;
import io.vertx.ext.auth.webauthn.WebAuthnOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.WebAuthnHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

import java.util.ArrayList;
import java.util.List;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> start) {

    final String origin = config().getString("origin");
    final boolean ssl = config().getBoolean("ssl", false);

    if (origin == null) {
      start.fail("Missing origin config");
      return;
    }

    final FileSystem fs = vertx.fileSystem();

    // Dummy database, real world workloads
    // use a persistent store or course!
    final InMemoryStore database = new InMemoryStore();

    // create the webauthn security object
    WebAuthn webAuthN = WebAuthn.create(
      vertx,
      new WebAuthnOptions(config().getJsonObject("webauthn", new JsonObject())))
      // where to load/update authenticators data
      .authenticatorFetcher(database::fetcher)
      .authenticatorUpdater(database::updater);

    // do we want to load custom metadata statements?
    if (fs.existsBlocking("metadataStatements")) {
      for (String f : fs.readDirBlocking("metadataStatements")) {
        System.out.println("Loading metadata statement: " + f);
        webAuthN.metaDataService()
          .addStatement(new JsonObject(fs.readFileBlocking(f)));
      }
    }

    List<Future> futures = new ArrayList<>();

    if (config().containsKey("mds")) {
      JsonObject mds = config().getJsonObject("mds");
      // do we want to load custom MDS2 servers?
      if (mds.containsKey("servers")) {
        mds.getJsonArray("servers").forEach(el -> {
          System.out.println("Loading toc: " + el);
          futures.add(webAuthN.metaDataService().fetchTOC((String) el));
        });
      }
    }

    CompositeFuture.all(futures)
      .onFailure(start::fail)
      .onSuccess(done -> {
        final Router app = Router.router(vertx);
        // serve the SPA
        app.route()
          .handler(StaticHandler.create());
        // parse the BODY
        app.post()
          .handler(BodyHandler.create());
        // add a session handler
        app.route()
          .handler(SessionHandler
            .create(LocalSessionStore.create(vertx)));

        // security handler
        WebAuthnHandler webAuthnHandler = WebAuthnHandler.create(webAuthN)
          .setOrigin(origin)
          // required callback
          .setupCallback(app.post("/webauthn/callback"))
          // optional register callback
          .setupCredentialsCreateCallback(app.post("/webauthn/register"))
          // optional login callback
          .setupCredentialsGetCallback(app.post("/webauthn/login"));

        // secure the remaining routes
        app.route().handler(webAuthnHandler);

        app.route("/protected")
          .handler(ctx -> {
            ctx.response()
              .putHeader("Content-Type", "text/plain; charset=UTF-8")
              .end(
                "▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄\n" +
                  "█░░░░░░░░▀█▄▀▄▀██████░▀█▄▀▄▀██████\n" +
                  "░░░░ ░░░░░░░▀█▄█▄███▀░░░ ▀█▄█▄███\n" +
                  "\n" +
                  "FIDO2 is Awesome!\n" +
                  "No Password phishing here!\n");
          });

        vertx.createHttpServer(
          ssl ?
            new HttpServerOptions()
              .setSsl(true)
              .setKeyStoreOptions(
                new JksOptions(config().getJsonObject("keystore"))) :
            new HttpServerOptions())
          .requestHandler(app)
          .listen(config().getInteger("port"), "0.0.0.0")
          .onSuccess(v -> {
            System.out.println("Server listening at: " + origin);
            start.complete();
          })
          .onFailure(err -> {
            if (err.getCause() instanceof FileSystemException) {
              System.err.println("Error reading the keystore, to create one do the following:");
              System.err.println("# replace the CN with your own IP address (other than localhost) with suffix .xip.io");
              System.err.println("keytool -genkeypair -alias rsakey -keyalg rsa -storepass password -keystore server.jks -storetype JKS -dname \"CN=192.168.178.74.xip.io,O=Vert.x Demo Server\"");
              System.err.println("# convert to PKCS#12 format for compatibility reasons");
              System.err.println("keytool -importkeystore -srckeystore server.jks -destkeystore server.jks -deststoretype pkcs12");
              System.err.println("# your new ssl certificate is on the file `server.jks`");
            }
            start.fail(err);
          });
      });
  }
}
