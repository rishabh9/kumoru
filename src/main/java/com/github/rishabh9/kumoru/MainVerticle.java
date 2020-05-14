package com.github.rishabh9.kumoru;

import com.github.rishabh9.kumoru.handlers.FinalHandler;
import com.github.rishabh9.kumoru.handlers.IndexSearchHandler;
import com.github.rishabh9.kumoru.handlers.JCenterMirrorHandler;
import com.github.rishabh9.kumoru.handlers.JitPackMirrorHandler;
import com.github.rishabh9.kumoru.handlers.LocalResourceHandler;
import com.github.rishabh9.kumoru.handlers.MavenMirrorHandler;
import com.github.rishabh9.kumoru.handlers.SendFileHandler;
import com.github.rishabh9.kumoru.handlers.ValidRequestHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.LoggerHandler;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(final Promise<Void> startFuture) {

    // Get all environment variables
    final int port = getKumoruPort();
    final boolean enableAccessLog = enableAccessLog();

    // All handlers
    final ValidRequestHandler validRequestHandler = new ValidRequestHandler();
    final LocalResourceHandler localResourceHandler = new LocalResourceHandler(vertx);
    final MavenMirrorHandler mavenMirror = new MavenMirrorHandler(vertx);
    final JCenterMirrorHandler jcenterMirror = new JCenterMirrorHandler(vertx);
    final JitPackMirrorHandler jitPackMirror = new JitPackMirrorHandler(vertx);
    final FinalHandler finalHandler = new FinalHandler();
    final IndexSearchHandler searchHandler = new IndexSearchHandler();
    final SendFileHandler sendFileHandler = new SendFileHandler();

    final Router router = Router.router(vertx);
    // for all routes do
    final Route route = router.route();
    if (enableAccessLog) {
      route.handler(LoggerHandler.create(LoggerFormat.DEFAULT));
    }
    route.handler(validRequestHandler);
    // for HEAD method do
    router.head().handler(searchHandler).handler(finalHandler);
    // for GET method do
    router
        .get()
        .handler(localResourceHandler)
        .handler(mavenMirror)
        .handler(jcenterMirror)
        .handler(jitPackMirror)
        .handler(sendFileHandler)
        .handler(finalHandler);

    // Logging network server activity
    final HttpServerOptions options = new HttpServerOptions().setLogActivity(true);
    final HttpServer httpServer = vertx.createHttpServer(options);
    httpServer.requestHandler(router);
    httpServer.listen(
        port,
        asyncResult -> {
          if (asyncResult.succeeded()) {
            startFuture.complete();
            log.info("Kumoru server started on port {}", port);
          } else {
            startFuture.fail(asyncResult.cause());
          }
        });
  }

  private boolean enableAccessLog() {
    final String flag = System.getenv("KUMORU_ACCESS_LOG");
    if (null != flag && !flag.isEmpty() && flag.matches("^(true|false)$")) {
      return Boolean.parseBoolean(flag);
    }
    return false;
  }

  // I see no utility of this facility if deployed as a Docker container.
  private int getKumoruPort() {
    final int defaultPort = 8888;
    final String port = System.getenv("KUMORU_PORT");
    log.debug("Port configured as {}", port);
    if (null != port
        && !port.isEmpty()
        && port.matches(
            "^()([1-9]|[1-5]?[0-9]{2,4}|6[1-4][0-9]{3}|65[1-4][0-9]{2}|655[1-2][0-9]|6553[1-5])$")) {
      return Integer.parseInt(port);
    } else {
      log.debug("Setting default port {}", defaultPort);
      return defaultPort;
    }
  }
}
