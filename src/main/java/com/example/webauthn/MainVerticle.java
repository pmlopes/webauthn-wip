package com.example.webauthn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.auth.webauthn.RelyingParty;
import io.vertx.ext.auth.webauthn.WebAuthn;
import io.vertx.ext.auth.webauthn.WebAuthnOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.WebAuthnHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

public class MainVerticle extends AbstractVerticle {

  public static void main(String[] args) {
    Vertx.vertx().deployVerticle(new MainVerticle());
  }

  @Override
  public void start() {

    final Router app = Router.router(vertx);
    // serve the SPA
    app.route()
      .handler(StaticHandler.create());
    // create the webauthn security object
    WebAuthn webAuthN = WebAuthn.create(
      vertx,
      new WebAuthnOptions()
        .setRelyingParty(
          new RelyingParty().setName("Vert.x WebAuthN Demo")))
      // where to load/update authenticators data
      .setAuthenticatorStore(new InMemoryStore());

    // parse the BODY
    app.post()
      .handler(BodyHandler.create());
    // add a session handler
    app.route()
      .handler(SessionHandler
        .create(LocalSessionStore.create(vertx)));

    // security handler
    WebAuthnHandler webAuthnHandler = WebAuthnHandler.create(webAuthN)
      .setOrigin("https://192.168.178.74.xip.io:8443")
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
      new HttpServerOptions()
        .setSsl(true)
        .setKeyStoreOptions(
          new JksOptions()
            .setPath("mytestkeys.jks")
            .setPassword("passphrase")))
      .requestHandler(app)
      .listen(8443, res -> {
        if (res.failed()) {
          res.cause().printStackTrace();
        } else {
          System.out.println("Server listening at: https://192.168.178.74.xip.io:8443/");
        }
      });
  }
}
