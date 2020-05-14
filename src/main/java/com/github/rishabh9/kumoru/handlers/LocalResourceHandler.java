package com.github.rishabh9.kumoru.handlers;

import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class LocalResourceHandler extends KumoruHandler {

  private final Vertx vertx;

  public LocalResourceHandler(final Vertx vertx) {
    this.vertx = vertx;
  }

  /**
   * This handler searches for the resource locally before checking the mirrors.
   *
   * @param routingContext The routing context being processed
   */
  public void handle(final RoutingContext routingContext) {
    final FileSystem fileSystem = vertx.fileSystem();
    final String path = routingContext.normalisedPath();
    final String absolutePath = REPO_ROOT + path;
    fileSystem.exists(
        absolutePath,
        asyncResult -> {
          if (null != asyncResult.result() && asyncResult.result()) {
            log.debug("Found resource {} locally", path);
            markResourceFound(routingContext);
          } else {
            log.debug("Resource {} not found locally, moving onto next handler", path);
          }
          routingContext.response().setChunked(true);
          routingContext.next();
        });
  }
}
