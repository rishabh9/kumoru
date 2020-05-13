package com.github.rishabh9.kumoru;

import com.github.rishabh9.kumoru.handlers.FileNotFoundErrorHandler;
import com.github.rishabh9.kumoru.handlers.FileSystemHandler;
import com.github.rishabh9.kumoru.handlers.IndexSearchHandler;
import com.github.rishabh9.kumoru.handlers.JCenterMirror;
import com.github.rishabh9.kumoru.handlers.JitPackMirror;
import com.github.rishabh9.kumoru.handlers.MavenMirror;
import com.github.rishabh9.kumoru.handlers.RequestValidator;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.LoggerHandler;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class MainVerticle extends AbstractVerticle {

  private static final int PORT = 8888;

  @Override
  public void start(final Promise<Void> startFuture) {

    // All handlers
    final RequestValidator requestValidator = new RequestValidator();
    final FileSystemHandler fileService = new FileSystemHandler(vertx);
    final MavenMirror mavenMirror = new MavenMirror(vertx);
    final JCenterMirror jcenterMirror = new JCenterMirror(vertx);
    final JitPackMirror jitPackMirror = new JitPackMirror(vertx);
    final FileNotFoundErrorHandler fileNotFoundErrorHandler = new FileNotFoundErrorHandler(vertx);
    final IndexSearchHandler searchHandler = new IndexSearchHandler(vertx);

    final Router router = Router.router(vertx);
    // for all routes do
    router.route().handler(LoggerHandler.create(LoggerFormat.DEFAULT)).handler(requestValidator);
    // for HEAD method do
    router.head().handler(searchHandler).handler(fileNotFoundErrorHandler);
    // for GET method do
    router
        .get()
        .handler(fileService)
        .handler(mavenMirror)
        .handler(jcenterMirror)
        .handler(jitPackMirror)
        .handler(fileNotFoundErrorHandler);

    // Logging network server activity
    final HttpServer httpServer = vertx.createHttpServer();
    httpServer.requestHandler(router);
    httpServer.listen(
        PORT,
        asyncResult -> {
          if (asyncResult.succeeded()) {
            startFuture.complete();
            log.info("Kumoru server started on port {}", PORT);
          } else {
            startFuture.fail(asyncResult.cause());
          }
        });
  }
}
