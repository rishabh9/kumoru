package com.github.rishabh9.kumoru.web.handlers;

import com.github.rishabh9.kumoru.common.KumoruCommon;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class UploadHandler extends KumoruHandler {

  private final Vertx vertx;

  public UploadHandler(final Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public void handle(final RoutingContext routingContext) {
    final String path = routingContext.normalisedPath();
    final String subPath = path.substring(0, path.lastIndexOf("/"));
    final String directory = KumoruCommon.REPO_ROOT + subPath;
    vertx
        .fileSystem()
        .exists(
            directory,
            existsResult -> {
              if (existsResult.succeeded()
                  && null != existsResult.result()
                  && existsResult.result()) {
                log.debug("The directory {} already exists", subPath);
                writeFile(routingContext);
              } else {
                log.debug("The directory {} does not exists", subPath);
                createDirectory(routingContext, directory);
              }
            });
  }

  private void createDirectory(final RoutingContext routingContext, final String directory) {
    vertx
        .fileSystem()
        .mkdirs(
            directory,
            mkdirsResult -> {
              if (mkdirsResult.succeeded()) {
                log.debug("Created directory {}", directory);
                writeFile(routingContext);
              } else {
                log.error("Failed to create directory {}", directory, mkdirsResult.cause());
                routingContext
                    .response()
                    .setStatusCode(INTERNAL_ERROR)
                    .setStatusMessage(mkdirsResult.cause().getMessage())
                    .end();
              }
            });
  }

  private void writeFile(final RoutingContext routingContext) {
    final String path = routingContext.normalisedPath();
    vertx
        .fileSystem()
        .writeFile(
            KumoruCommon.REPO_ROOT + path,
            routingContext.getBody(),
            writeResult -> {
              if (writeResult.succeeded()) {
                log.debug("Saved {} to disk", path);
                routingContext.response().end();
              } else {
                log.error("There was error writing {} to disk", path, writeResult.cause());
                routingContext
                    .response()
                    .setStatusCode(INTERNAL_ERROR)
                    .setStatusMessage(writeResult.cause().getMessage())
                    .end();
              }
            });
  }
}
