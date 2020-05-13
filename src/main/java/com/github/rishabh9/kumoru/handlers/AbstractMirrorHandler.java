package com.github.rishabh9.kumoru.handlers;

import com.github.rishabh9.kumoru.VersionProperties;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import lombok.extern.log4j.Log4j2;

@Log4j2
public abstract class AbstractMirrorHandler extends KumoruHandler {

  private final Vertx vertx;
  private final String releaseUrl;
  private final String snapshotUrl;

  /**
   * Constructor.
   *
   * @param vertx The Vertx object.
   * @param releaseUrl The URL of Release Repository.
   * @param snapshotUrl The URL of Snapshot repository.
   */
  public AbstractMirrorHandler(
      final Vertx vertx, final String releaseUrl, final String snapshotUrl) {
    log.info("Vertx init: {}", vertx);
    this.vertx = vertx;
    this.releaseUrl = releaseUrl;
    this.snapshotUrl = snapshotUrl;
  }

  @Override
  public void handle(final RoutingContext routingContext) {
    if (resourceFound(routingContext)) {
      log.debug("Resource has been found, moving onto next handler");
      routingContext.next();
    } else {
      final HttpServerRequest req = routingContext.request();
      final String path = req.path();
      final String url = path.contains("SNAPSHOT") ? snapshotUrl : releaseUrl;

      createWebClient(url)
          .get(path)
          .expect(ResponsePredicate.SC_SUCCESS)
          .as(BodyCodec.buffer())
          .send(
              asyncWebResult -> {
                if (asyncWebResult.succeeded()) {
                  log.debug("Found resource {} on {}", path, url);
                  vertx
                      .fileSystem()
                      .writeFile(
                          REPO_DIR + path,
                          asyncWebResult.result().body(),
                          writeResult -> {
                            if (writeResult.succeeded()) {
                              markResourceFound(routingContext);
                              log.debug("Saved {} from {} to disk", path, url);
                              routingContext.next();
                            } else {
                              log.error(
                                  "There was error writing {} from {} to disk",
                                  path,
                                  url,
                                  writeResult.cause());
                              routingContext
                                  .response()
                                  .setStatusCode(INTERNAL_ERROR)
                                  .setStatusMessage(writeResult.cause().getMessage())
                                  .end();
                            }
                          });
                } else {
                  log.debug("Not able to retrieve {} from {}", path, url, asyncWebResult.cause());
                  routingContext.next();
                }
              });
    }
  }

  private WebClient createWebClient(final String host) {
    final WebClientOptions webClientOptions =
        new WebClientOptions()
            .setDefaultHost(host)
            .setSsl(true)
            .setUserAgent("kumoru/" + VersionProperties.INSTANCE.getVersion())
            .setFollowRedirects(true)
            .setMaxRedirects(5);
    log.info("Version: {}", VersionProperties.INSTANCE.getVersion());
    return WebClient.create(vertx, webClientOptions);
  }
}
