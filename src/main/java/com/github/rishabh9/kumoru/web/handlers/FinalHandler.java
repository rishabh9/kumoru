package com.github.rishabh9.kumoru.web.handlers;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class FinalHandler extends KumoruHandler {

  /**
   * If you have reached this handler, it means the server is not able to process the request at
   * all. This is the final handler for every requests and default to returning a 404 as response.
   *
   * @param routingContext The routing context being processed.
   */
  @Override
  public void handle(final RoutingContext routingContext) {
    final HttpServerRequest req = routingContext.request();
    log.warn(
        "Unable to process {} {} originating from {} {} {}",
        req.method().toString(),
        req.path(),
        req.remoteAddress().host(),
        req.remoteAddress().port(),
        req.remoteAddress().path());
    routingContext.response().setStatusCode(NOT_FOUND).setStatusMessage("Resource not found").end();
  }
}
