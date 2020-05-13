package com.github.rishabh9.kumoru.handlers;

import com.github.rishabh9.kumoru.ApplicationProperties;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class AbstractMirror implements Handler<RoutingContext> {

  private final Vertx vertx;
  private final String releaseUrl;
  private final String snapshotUrl;
  private final WebClient releaseServerClient;
  private final WebClient snapshotServerClient;

  /**
   * Constructor.
   *
   * @param vertx The Vertx object.
   * @param releaseUrl The URL of Release Repository.
   * @param snapshotUrl The URL of Snapshot repository.
   */
  public AbstractMirror(final Vertx vertx, final String releaseUrl, final String snapshotUrl) {
    log.info("Vertx init: {}", vertx);
    this.vertx = vertx;
    this.releaseUrl = releaseUrl;
    this.snapshotUrl = snapshotUrl;
    this.releaseServerClient = createWebClient(releaseUrl);
    this.snapshotServerClient = createWebClient(snapshotUrl);
  }

  @Override
  public void handle(final RoutingContext routingContext) {
    final HttpServerRequest req = routingContext.request();
    final String path = req.path();
    final WebClient webClient =
        path.contains("SNAPSHOT") ? snapshotServerClient : releaseServerClient;
    webClient.get(path);
  }

  private WebClient createWebClient(final String host) {
    final WebClientOptions webClientOptions =
        new WebClientOptions()
            .setDefaultHost(host)
            .setSsl(true)
            .setUserAgent("kumoru/" + ApplicationProperties.INSTANCE.getVersion());
    log.info("Version: {}", ApplicationProperties.INSTANCE.getVersion());
    return WebClient.create(vertx, webClientOptions);
  }
}
