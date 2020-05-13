package com.github.rishabh9.kumoru.handlers;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class RequestValidator implements Handler<RoutingContext> {

  private static final int BAD_REQUEST = 400;

  @Override
  public void handle(final RoutingContext routingContext) {
    final HttpServerRequest req = routingContext.request();
    final String path = req.path();
    if (isValidPath(path)) {
      routingContext.next();
    } else {
      log.error("Path has invalid characters: {}", path);
      req.response()
          .setStatusCode(BAD_REQUEST)
          .setStatusMessage("Path has invalid characters")
          .end();
    }
  }

  private boolean isValidPath(final String path) {
    return path.matches("[a-zA-z0-9.-_/]+");
  }
}
