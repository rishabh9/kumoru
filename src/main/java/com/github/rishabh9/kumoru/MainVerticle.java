package com.github.rishabh9.kumoru;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import lombok.extern.log4j.Log4j2;
import org.apache.tika.Tika;

@Log4j2
public class MainVerticle extends AbstractVerticle {

  private static final int PORT = 8888;
  private static final int NOT_FOUND = 404;
  private static final String ROOT_DIR = "/tmp/vrt";

  @Override
  public void start(final Promise<Void> startFuture) throws Exception {
    // Logging network server activity
    final HttpServerOptions options = new HttpServerOptions().setLogActivity(true);
    final HttpServer httpServer = vertx.createHttpServer(options);
    final FileSystem fileSystem = vertx.fileSystem();

    httpServer.requestHandler(req -> handleRequest(fileSystem, req));

    httpServer.listen(
        PORT,
        asyncResult -> {
          if (asyncResult.succeeded()) {
            startFuture.complete();
            log.info("Kumoru server started on port {}", PORT);
          } else {
            startFuture.fail(asyncResult.cause());
          }
        });
  }

  private void handleRequest(final FileSystem fileSystem, final HttpServerRequest req) {
    final String path = req.path();
    final String method = req.rawMethod();
    log.info("Requesting: {}: {}", method, path);
    final String absolutePath = ROOT_DIR + path;
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
    log.info("Exiting request handler here...");
  }
}
