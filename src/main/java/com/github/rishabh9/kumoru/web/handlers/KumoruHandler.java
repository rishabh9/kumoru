package com.github.rishabh9.kumoru.web.handlers;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

abstract class KumoruHandler implements Handler<RoutingContext> {
  protected static final int BAD_REQUEST = 400;
  protected static final int NOT_FOUND = 404;
  protected static final int METHOD_NOT_ALLOWED = 405;
  protected static final int INTERNAL_ERROR = 500;
  private static final String RESOURCE_FOUND_FLAG = "KUMORU_RESOURCE_FOUND";

  protected void markResourceFound(final RoutingContext routingContext) {
    routingContext.data().put(RESOURCE_FOUND_FLAG, true);
  }

  protected Boolean resourceFound(final RoutingContext routingContext) {
    return (Boolean) routingContext.data().getOrDefault(RESOURCE_FOUND_FLAG, false);
  }
}
