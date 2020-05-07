package com.github.rishabh9.kumoru;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

public class MainVerticle extends AbstractVerticle {

  private static final int PORT = 8888;

  @Override
  public void start(final Future<Void> startFuture) throws Exception {
    vertx
        .createHttpServer()
        .requestHandler(
            req -> {
              req.response().putHeader("content-type", "text/plain").end("Hello from Vert.x!");
            })
        .listen(
            PORT,
            http -> {
              if (http.succeeded()) {
                startFuture.complete();
                System.out.println("HTTP server started on port 8888");
              } else {
                startFuture.fail(http.cause());
              }
            });
  }
}
