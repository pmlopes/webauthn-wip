package com.example.webauthn;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.webauthn.WebAuthN;
import io.vertx.ext.auth.webauthn.WebAuthNOptions;
import io.vertx.ext.auth.webauthn.WebAuthNStore;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.sstore.SessionStore;

import java.util.HashMap;
import java.util.Map;

public class MainVerticle extends AbstractVerticle {


  @Override
  public void start() {

    WebAuthN webAuthN = WebAuthN.create(vertx, new WebAuthNOptions().setRealm("Vertx Examples Realm"));

    webAuthN.webAuthNStore(new WebAuthNStore() {
      final Map<String, JsonObject> database = new HashMap<>();
      @Override
      public WebAuthNStore find(String id, Handler<AsyncResult<JsonObject>> handler) {
        handler.handle(Future.succeededFuture(database.get(id)));
        return this;
      }
      @Override
      public WebAuthNStore update(String id, JsonObject data, Handler<AsyncResult<JsonObject>> handler) {
        handler.handle(Future.succeededFuture(database.put(id, data)));
        return this;
      }
    });

    WebAuthNHandler webAuthNHandler = WebAuthNHandler.create(webAuthN, "http://localhost:8080");


    final Router router = Router.router(vertx);
    // I/O requires HTTP posts
    router.route().handler(BodyHandler.create());
    // State is stored in the session
    router.route().handler(CookieHandler.create());
    router.route().handler(SessionHandler.create(SessionStore.create(vertx)));
    // We need a user session handler too to make sure
    // the user is stored in the session between requests
    router.route()
      .handler(UserSessionHandler.create(webAuthN));

    // default routes

    /* Returns if user is logged in */
    router.get("/isLoggedIn").handler(ctx -> {
      if (ctx.user() == null) {
        ctx.response()
          .putHeader("Content-Type", "application/json")
          .end(new JsonObject().put("status", "failed").encode());
      } else {
        ctx.response()
          .putHeader("Content-Type", "application/json")
          .end(new JsonObject().put("status", "ok").encode());
      }
    });

    /* Logs user out */
    router.get("/logout").handler(ctx -> {
      if (ctx.session() != null) {
        ctx.session().destroy();
      }

      ctx.response()
        .putHeader("Content-Type", "application/json")
        .end(new JsonObject().put("status", "ok").encode());
    });

    router.post("/webauthn/register").handler(webAuthNHandler.registerHandler());
    router.post("/webauthn/login").handler(webAuthNHandler.loginHandler());
    router.post("/webauthn/response").handler(webAuthNHandler);

    // secure the rest

    /* Returns personal info and THE SECRET INFORMATION */
    router.get("/personalInfo").handler(ctx -> {
      if (ctx.user() == null) {
        ctx.response()
          .putHeader("Content-Type", "application/json")
          .end(new JsonObject().put("status", "failed").put("message", "Access denied").encode());
      } else {
        ctx.response()
          .putHeader("Content-Type", "application/json")
          .end(new JsonObject()
            .put("status", "ok")
            .put("'name'", ctx.user().principal().getString("name"))
            .put("theSecret", "<img width=\"250px\" src=\"img/theworstofthesecrets.jpg\">").encode());
      }
    });


    router.route().handler(StaticHandler.create());

    vertx.createHttpServer().requestHandler(router).listen(8080, res -> {
      if (res.failed()) {
        res.cause().printStackTrace();
      } else {
        System.out.println("Server listening at: http://localhost:8080/");
      }
    });
  }

  public static void main(String[] args) {
    Vertx.vertx().deployVerticle(new MainVerticle());
  }
}
