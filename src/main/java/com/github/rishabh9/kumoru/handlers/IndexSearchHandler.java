package com.github.rishabh9.kumoru.handlers;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

public class IndexSearchHandler implements Handler<RoutingContext> {

  private final Vertx vertx;

  public IndexSearchHandler(final Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public void handle(final RoutingContext event) {}
}
