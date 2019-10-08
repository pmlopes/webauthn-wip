package com.example.webauthn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.auth.webauthn.AuthenticatorAttachment;
import io.vertx.ext.auth.webauthn.UserVerification;
import io.vertx.ext.auth.webauthn.WebAuthN;
import io.vertx.ext.auth.webauthn.WebAuthNOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.handler.WebAuthNHandler;

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
    WebAuthN webAuthN = WebAuthN.create(
      vertx,
      new WebAuthNOptions()
        .setOrigin("https://192.168.178.74.xip.io:8443")
        .setRpName("Vert.x WebAuthN Demo")
        // What kind of authentication do you want? do you care? if you care you can specify it
        // # security keys
        .setAuthenticatorAttachment(AuthenticatorAttachment.CROSS_PLATFORM)
        .setRequireResidentKey(false)
        // # fingerprint
//        .setAuthenticatorAttachment(AuthenticatorAttachment.PLATFORM)
//        .setRequireResidentKey(false)
//        .setUserVerification(UserVerification.REQUIRED)
        ,
      new InMemmoryStore());
    // parse the BODY
    app.post()
      .handler(BodyHandler.create());
    // add a session handler
    app.route()
      .handler(SessionHandler
        .create(LocalSessionStore.create(vertx))
        .setAuthProvider(webAuthN));

    // security handler
    WebAuthNHandler webAuthNHandler = WebAuthNHandler.create(webAuthN)
      // required callback
      .setupCallback(app.post("/webauthn/response"))
      // optional register callback
      .setupCredentialsCreateCallback(app.post("/webauthn/register"))
      // optional login callback
      .setupCredentialsGetCallback(app.post("/webauthn/login"));

    // secure the remaining routes
    app.route().handler(webAuthNHandler);

    app.route("/protected")
      .handler(ctx -> {
        System.out.println("USER: " + ctx.user());
        ctx.end("Protected resource");
      });


    // we need HTTPs to have credentials available
    vertx.createHttpServer(
      new HttpServerOptions()
        .setSsl(true)
        .setKeyStoreOptions(
          new JksOptions()
            .setPath("192.168.178.74.xip.io.jks")
            .setPassword("passphrase")))
      .requestHandler(app)
      .listen(8443, res -> {
      if (res.failed()) {
        res.cause().printStackTrace();
      } else {
        System.out.println("Server listening at: https://192.168.178.74.xip.io:8443");
      }
    });
  }
}
