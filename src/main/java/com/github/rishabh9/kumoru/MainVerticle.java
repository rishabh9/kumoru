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
    router.route().handler(LoggerHandler.create(LoggerFormat.DEFAULT)).handler(validRequestHandler);
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
