package com.github.rishabh9.kumoru.handlers;

import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ValidRequestHandler extends KumoruHandler {

  @Override
  public void handle(final RoutingContext routingContext) {
    final String path = routingContext.normalisedPath();
    if (isValidPath(path)) {
      routingContext.next();
    } else {
      log.error("Path has invalid characters: {}", path);
      routingContext
          .response()
          .setStatusCode(BAD_REQUEST)
          .setStatusMessage("Path has invalid characters")
          .end();
    }
  }

  private boolean isValidPath(final String path) {
    return path.matches("[a-zA-z0-9.\\-_/]+");
  }
}
