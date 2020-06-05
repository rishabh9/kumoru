package com.github.rishabh9.kumoru.web.handlers;

import static com.github.rishabh9.kumoru.common.KumoruCommon.REPO_ROOT;
import static com.github.rishabh9.kumoru.common.KumoruCommon.createWebClient;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
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
      final String path = routingContext.normalisedPath();
      final String url = path.contains("SNAPSHOT") ? snapshotUrl : releaseUrl;

      createWebClient(vertx)
          .getAbs(url + path)
          .expect(ResponsePredicate.SC_SUCCESS)
          .as(BodyCodec.buffer())
          .send(
              asyncWebResult -> {
                if (asyncWebResult.succeeded()
                    && null != asyncWebResult.result()
                    && null != asyncWebResult.result().body()) {
                  log.debug("Found resource {} on {}", path, url);
                  saveResource(routingContext, path, url, asyncWebResult.result().body());
                } else {
                  log.debug("Not able to retrieve {} from {}", path, url, asyncWebResult.cause());
                  routingContext.next();
                }
              });
    }
  }

  private void saveResource(
      final RoutingContext routingContext,
      final String path,
      final String url,
      final Buffer buffer) {
    final String dir = path.substring(0, path.lastIndexOf("/"));
    vertx
        .fileSystem()
        .mkdirs(
            REPO_ROOT + dir,
            mkdirsResult -> {
              if (mkdirsResult.succeeded()) {
                log.debug("Created directory {}", dir);
                writeFile(routingContext, path, url, buffer);
              } else {
                log.error("Failed to create directory {}", dir, mkdirsResult.cause());
                routingContext
                    .response()
                    .setStatusCode(INTERNAL_ERROR)
                    .setStatusMessage(mkdirsResult.cause().getMessage())
                    .end();
              }
            });
  }

  private void writeFile(
      final RoutingContext routingContext,
      final String path,
      final String url,
      final Buffer buffer) {
    vertx
        .fileSystem()
        .writeFile(
            REPO_ROOT + path,
            buffer,
            writeResult -> {
              if (writeResult.succeeded()) {
                markResourceFound(routingContext);
                log.debug("Saved {} from {} to disk", path, url);
                routingContext.next();
              } else {
                log.error(
                    "There was error writing {} from {} to disk", path, url, writeResult.cause());
                routingContext
                    .response()
                    .setStatusCode(INTERNAL_ERROR)
                    .setStatusMessage(writeResult.cause().getMessage())
                    .end();
              }
            });
  }
}
