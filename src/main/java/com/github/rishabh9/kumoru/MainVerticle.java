package com.github.rishabh9.kumoru;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class MainVerticle extends AbstractVerticle {

  private static final int PORT = 8888;

  @Override
  public void start(final Promise<Void> startFuture) throws Exception {
    vertx
        .createHttpServer()
        .requestHandler(
            req -> {
              req.response().putHeader("content-type", "text/plain").end(req.path());
            })
        .listen(
            PORT,
            http -> {
              if (http.succeeded()) {
                startFuture.complete();
                log.info("HTTP server started on port 8888");
              } else {
                startFuture.fail(http.cause());
              }
            });
  }
}
