package com.github.rishabh9.kumoru;

import com.github.rishabh9.kumoru.handlers.FinalHandler;
import com.github.rishabh9.kumoru.handlers.JCenterMirrorHandler;
import com.github.rishabh9.kumoru.handlers.JitPackMirrorHandler;
import com.github.rishabh9.kumoru.handlers.LocalResourceHandler;
import com.github.rishabh9.kumoru.handlers.MavenMirrorHandler;
import com.github.rishabh9.kumoru.handlers.SendFileHandler;
import com.github.rishabh9.kumoru.handlers.UploadHandler;
import com.github.rishabh9.kumoru.handlers.ValidRequestHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.LoggerHandler;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(final Promise<Void> startFuture) {

    // Get all environment variables
    final int port = getKumoruPort();
    final Router router = configureRoutes(enableAccessLog(), getBodyLimit());

    // Logging network server activity
    final HttpServerOptions options = new HttpServerOptions().setLogActivity(true);
    final HttpServer httpServer = vertx.createHttpServer(options);
    httpServer.requestHandler(router);
    httpServer.listen(
        port,
        asyncResult -> {
          if (asyncResult.succeeded()) {
            startFuture.complete();
            log.info(
                "Kumoru server [v{}] started on port {}",
                VersionProperties.INSTANCE.getVersion(),
                port);
          } else {
            startFuture.fail(asyncResult.cause());
          }
        });
  }

  private Router configureRoutes(final boolean enableAccessLog, final long bodyLimit) {

    // All handlers
    final ValidRequestHandler validRequestHandler = new ValidRequestHandler();
    final LocalResourceHandler localResourceHandler = new LocalResourceHandler(vertx);
    final MavenMirrorHandler mavenMirror = new MavenMirrorHandler(vertx);
    final JCenterMirrorHandler jcenterMirror = new JCenterMirrorHandler(vertx);
    final JitPackMirrorHandler jitPackMirror = new JitPackMirrorHandler(vertx);
    final SendFileHandler sendFileHandler = new SendFileHandler();
    final FinalHandler finalHandler = new FinalHandler();
    final UploadHandler uploadHandler = new UploadHandler(vertx);

    final Router router = Router.router(vertx);
    // for all routes do
    final Route route = router.route();
    if (enableAccessLog) {
      route.handler(LoggerHandler.create(LoggerFormat.DEFAULT));
    }
    route.handler(validRequestHandler);

    // for GET method do
    router
        .get()
        .handler(localResourceHandler)
        .handler(mavenMirror)
        .handler(jcenterMirror)
        .handler(jitPackMirror)
        .handler(sendFileHandler)
        .handler(finalHandler);

    // for PUT method do
    router
        .put()
        .handler(BodyHandler.create().setBodyLimit(bodyLimit).setDeleteUploadedFilesOnEnd(true))
        .handler(uploadHandler);

    return router;
  }

  private int getBodyLimit() {
    // Body limited to 50MB
    final int defaultBodyLimit = 50000000;
    final String bodyLimit = System.getenv("KUMORU_BODY_LIMIT");
    log.debug("Body limit configured as {} bytes", bodyLimit);
    if (null != bodyLimit && !bodyLimit.isEmpty() && !bodyLimit.matches(".*\\D.*")) {
      return Integer.parseInt(bodyLimit);
    } else {
      log.debug("Setting body limit as {} bytes", defaultBodyLimit);
      return defaultBodyLimit;
    }
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
