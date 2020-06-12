package com.github.rishabh9.kumoru.web.handlers;

import static com.github.rishabh9.kumoru.common.KumoruCommon.REPO_ROOT;
import static com.github.rishabh9.kumoru.common.KumoruCommon.createWebClient;

import com.github.rishabh9.kumoru.common.dto.BasicAuth;
import com.github.rishabh9.kumoru.common.dto.BearerToken;
import com.github.rishabh9.kumoru.common.dto.Repository;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class RepositoryHandler extends KumoruHandler {

  private final Vertx vertx;
  private final Repository repository;
  private final boolean snapshot;

  /**
   * Constructor.
   *
   * @param vertx The Vertx object.
   * @param repository The URL of repository.
   */
  public RepositoryHandler(final Vertx vertx, final Repository repository, final boolean snapshot) {
    this.vertx = vertx;
    this.repository = repository;
    this.snapshot = snapshot;
  }

  @Override
  public void handle(final RoutingContext routingContext) {
    if (resourceFound(routingContext)) {
      log.debug("[{}] Resource has been found, moving onto next handler", repository.getName());
      routingContext.next();
    } else if (ignore(routingContext)) {
      log.debug(
          "[{}] Snapshots not handled by this handler, moving onto next handler",
          repository.getName());
      routingContext.next();
    } else {
      final String path = routingContext.normalisedPath();

      final HttpRequest<Buffer> request =
          createWebClient(vertx)
              .getAbs(repository.getUrl() + path)
              .expect(ResponsePredicate.SC_SUCCESS)
              .as(BodyCodec.buffer());
      final BasicAuth basicAuth = repository.getBasicAuth();
      if (null != basicAuth) {
        request.basicAuthentication(basicAuth.getUsername(), basicAuth.getPassword());
      }
      final BearerToken bearerToken = repository.getBearerToken();
      if (null != bearerToken) {
        request.bearerTokenAuthentication(bearerToken.getToken());
      }
      request.send(
          asyncWebResult -> {
            if (asyncWebResult.succeeded()
                && null != asyncWebResult.result()
                && null != asyncWebResult.result().body()) {
              log.debug("[{}] Found resource {}", repository.getName(), path);
              saveResource(routingContext, path, asyncWebResult.result().body());
            } else {
              log.debug(
                  "[{}] Not able to retrieve {}",
                  repository.getName(),
                  path,
                  asyncWebResult.cause());
              routingContext.next();
            }
          });
    }
  }

  private boolean ignore(final RoutingContext routingContext) {
    return routingContext.normalisedPath().contains("-SNAPSHOT") && !snapshot;
  }

  private void saveResource(
      final RoutingContext routingContext, final String path, final Buffer buffer) {
    final String dir = path.substring(0, path.lastIndexOf("/"));
    vertx
        .fileSystem()
        .mkdirs(
            REPO_ROOT + dir,
            mkdirsResult -> {
              if (mkdirsResult.succeeded()) {
                log.debug("Created directory {}", dir);
                writeFile(routingContext, path, buffer);
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
      final RoutingContext routingContext, final String path, final Buffer buffer) {
    vertx
        .fileSystem()
        .writeFile(
            REPO_ROOT + path,
            buffer,
            writeResult -> {
              if (writeResult.succeeded()) {
                markResourceFound(routingContext);
                log.debug("Saved {} from {} to disk", path, repository.getUrl());
                routingContext.next();
              } else {
                log.error(
                    "There was error writing {} from {} to disk",
                    path,
                    repository.getUrl(),
                    writeResult.cause());
                routingContext
                    .response()
                    .setStatusCode(INTERNAL_ERROR)
                    .setStatusMessage(writeResult.cause().getMessage())
                    .end();
              }
            });
  }
}
