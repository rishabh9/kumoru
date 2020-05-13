package com.github.rishabh9.kumoru.handlers;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;
import org.apache.tika.Tika;

@Log4j2
public class FileSystemHandler implements Handler<RoutingContext> {

  private static final String REPO_DIR = "/srv/repo";
  private static final int NOT_FOUND = 404;

  private final Vertx vertx;

  public FileSystemHandler(final Vertx vertx) {
    this.vertx = vertx;
  }

  /**
   * h.
   *
   * @param routingContext The web routing context
   */
  public void handle(final RoutingContext routingContext) {
    final FileSystem fileSystem = vertx.fileSystem();
    final HttpServerRequest req = routingContext.request();
    final String path = req.path();
    log.info("Requesting for file with path - {}", path);
    final String absolutePath = REPO_DIR + path;
    fileSystem.exists(
        absolutePath,
        asyncResult -> {
          if (null != asyncResult.result() && asyncResult.result()) {
            final String filename = path.substring(path.lastIndexOf("/") + 1);
            final String mimeType = new Tika().detect(absolutePath);
            log.info("Filename: {}, MimeType: {}", filename, mimeType);
            req.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, mimeType)
                .putHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .putHeader(HttpHeaders.TRANSFER_ENCODING, "chunked")
                .sendFile(absolutePath);
          } else {
            log.info("File wasn't found: {}", absolutePath);
            req.response().setStatusCode(NOT_FOUND).setStatusMessage("Resource not found").end();
          }
        });
  }
}
