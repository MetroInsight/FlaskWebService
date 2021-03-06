package metroinsight.citadel.authorization;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.ext.web.templ.JadeTemplateEngine;
import metroinsight.citadel.common.MicroServiceVerticle;

public class AuthorizationVerticle extends MicroServiceVerticle {

  @Override
  public void start(Future<Void> fut) {
    // Create a router object.
    JsonObject configs = config();
    Router router = Router.router(vertx);
    GoogleLogin googlelogin = new GoogleLogin(vertx, configs);
    router.route().handler(CookieHandler.create());
    // Create a clustered session store using defaults
    SessionStore store = LocalSessionStore.create(vertx);
    SessionHandler sessionHandler = SessionHandler.create(store);
    // Make sure all requests are routed through the session handler too
    router.route().handler(sessionHandler);

    HttpServerOptions httpOptions = getBaseHttpOptions();
    httpOptions.setPort(configs.getInteger("auth.http.port", 8088)); // Default port 8088

    // In order to use a template we first need to create an engine
    final JadeTemplateEngine engine = JadeTemplateEngine.create();
    // Entry point to the application, this will render a custom template.
    router.route("/jade").handler(ctx -> {
      // we define a hardcoded title for our application, change it
      ctx.put("name", "Citadel");

      // and now delegate to the engine to render it.
      engine.render(ctx, "templates/test.jade", res -> {
        if (res.succeeded()) {
          ctx.response().end(res.result());
        } else {
          ctx.fail(res.cause());
        }
      });
    });

    router.route("/login*").handler(BodyHandler.create());
    router.route("/login").handler(googlelogin::DisplayLoginJade);

    router.route("/index*").handler(BodyHandler.create());
    router.route("/index").handler(googlelogin::DisplayIndexJade);

    // Bind "/" to our hello message - so we are still compatible.
    router.route("/").handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "text/html").end("<h1>Welcome to Citadel Authorization</h1>");
    });

    // Serve static resources from the /assets directory
    // router.route("/assets/*").handler(StaticHandler.create("assets"));
    router.route("/scripts/*").handler(StaticHandler.create("scripts"));

    // REST API routing for accepting google token
    router.route("/api/token*").handler(BodyHandler.create());
    router.post("/api/token").handler(googlelogin::DisplayToken);

    // Routing to display index page
    router.route("/logout*").handler(BodyHandler.create());
    router.post("/logout").handler(googlelogin::LogOut);

    // Create the HTTP server and pass the "accept" method to the request handler.
    vertx.createHttpServer(httpOptions).requestHandler(router::accept).listen(
        // Retrieve the port from the configuration,
        result -> {
          if (result.succeeded()) {
            fut.complete();
            System.out.println("AUTH_VERTICLE STARTED on " + Integer.toString(result.result().actualPort()));
          } else {
            fut.fail(result.cause());
          }
        });
  }// end start

}//end class