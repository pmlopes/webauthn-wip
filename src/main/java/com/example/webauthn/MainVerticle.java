package com.example.webauthn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.auth.webauthn.WebAuthn;
import io.vertx.ext.auth.webauthn.WebAuthnOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.sstore.LocalSessionStore;

import java.util.ArrayList;
import java.util.List;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> start) {

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
        // parse the BODY
        app.post()
          .handler(BodyHandler.create());
        // favicon
        app.route()
          .handler(FaviconHandler.create(vertx));
        // add a session handler
        app.route()
          .handler(SessionHandler
            .create(LocalSessionStore.create(vertx))
            .setCookieSameSite(CookieSameSite.STRICT));

        // security handler
        WebAuthnHandler webAuthnHandler = WebAuthnHandler.create(webAuthN)
          .setOrigin(String.format("https://%s.nip.io:8443", System.getenv("IP")))
          // required callback
          .setupCallback(app.post("/webauthn/callback"))
          // optional register callback
          .setupCredentialsCreateCallback(app.post("/webauthn/register"))
          // optional login callback
          .setupCredentialsGetCallback(app.post("/webauthn/login"));

        // secure the remaining routes
        app.route("/protected/*").handler(webAuthnHandler);

        // serve the SPA
        app.route()
          .handler(StaticHandler.create());

        vertx.createHttpServer(
          new HttpServerOptions()
            .setSsl(true)
            .setKeyStoreOptions(
              new JksOptions()
                .setPath("cert-store.jks")
                .setPassword(System.getenv("CERTSTORE_SECRET"))))

          .requestHandler(app)
          .listen(8443, "0.0.0.0")
          .onFailure(start::fail)
          .onSuccess(v -> {
            System.out.printf("Server listening at: https://%s.nip.io:8443%n", System.getenv("IP"));
            start.complete();
          });
      });
  }
}
