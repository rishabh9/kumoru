package com.github.rishabh9.kumoru.web;

import com.github.rishabh9.kumoru.common.KumoruConfig;
import com.github.rishabh9.kumoru.web.handlers.FinalHandler;
import com.github.rishabh9.kumoru.web.handlers.JCenterMirrorHandler;
import com.github.rishabh9.kumoru.web.handlers.JitPackMirrorHandler;
import com.github.rishabh9.kumoru.web.handlers.LocalResourceHandler;
import com.github.rishabh9.kumoru.web.handlers.MavenMirrorHandler;
import com.github.rishabh9.kumoru.web.handlers.SendFileHandler;
import com.github.rishabh9.kumoru.web.handlers.UploadHandler;
import com.github.rishabh9.kumoru.web.handlers.ValidRequestHandler;
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
public class WebServer extends AbstractVerticle {

  private HttpServer httpServer;

  @Override
  public void start(final Promise<Void> startFuture) {

    // Get configuration
    final int port = KumoruConfig.INSTANCE.getKumoruPort();
    final Router router = setupRoutes();

    // Logging network server activity
    final HttpServerOptions options = new HttpServerOptions().setLogActivity(true);
    httpServer = vertx.createHttpServer(options);
    httpServer.requestHandler(router);
    httpServer.listen(
        port,
        asyncResult -> {
          if (asyncResult.succeeded()) {
            log.debug("Web server running on port {}", port);
            startFuture.complete();
          } else {
            log.fatal("Failed to start web server", asyncResult.cause());
            startFuture.fail(asyncResult.cause());
          }
        });
  }

  @Override
  public void stop(final Promise<Void> stopPromise) {
    log.debug("Stopping web verticle...");
    if (null != httpServer) {
      httpServer.close(
          closeResult -> {
            if (closeResult.succeeded()) {
              log.debug("Web server stopped");
              stopPromise.complete();
            } else {
              log.fatal("Failed to stop web server", closeResult.cause());
              stopPromise.fail(closeResult.cause());
            }
          });
    }
  }

  private Router setupRoutes() {

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
    if (KumoruConfig.INSTANCE.isEnableAccessLog()) {
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
        .handler(
            BodyHandler.create()
                .setBodyLimit(KumoruConfig.INSTANCE.getBodyLimit())
                .setDeleteUploadedFilesOnEnd(true))
        .handler(uploadHandler);

    return router;
  }
}