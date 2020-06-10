package com.github.rishabh9.kumoru.web.handlers;

import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.PUT;

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ValidRequestHandler extends KumoruHandler {

  @Override
  public void handle(final RoutingContext routingContext) {
    if (!isSupportedMethod(routingContext.request().method())) {
      log.debug("Unsupported HTTP method: {}", routingContext.request().method());
      routingContext
          .response()
          .setStatusCode(METHOD_NOT_ALLOWED)
          .setStatusMessage("Method Not Allowed")
          .end();
    } else {
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
  }

  private boolean isSupportedMethod(final HttpMethod method) {
    return GET.equals(method) || PUT.equals(method);
  }

  private boolean isValidPath(final String path) {
    return path.matches("[a-zA-z0-9.\\-_/]+");
  }
}
