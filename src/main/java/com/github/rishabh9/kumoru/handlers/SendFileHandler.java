package com.github.rishabh9.kumoru.handlers;

import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;
import org.apache.tika.Tika;

@Log4j2
public class SendFileHandler extends KumoruHandler {

  /**
   * Sends the file found locally to the requesting client.
   *
   * @param routingContext The routing context being processed
   */
  public void handle(final RoutingContext routingContext) {
    if (resourceFound(routingContext)) {
      final String path = routingContext.normalisedPath();
      log.debug("Sending resource {}", path);
      final String absolutePath = REPO_ROOT + path;
      final String filename = path.substring(path.lastIndexOf("/") + 1);
      final String mimeType = new Tika().detect(absolutePath);
      log.debug("Filename: {}, MimeType: {}", filename, mimeType);
      routingContext
          .response()
          .putHeader(HttpHeaders.CONTENT_TYPE, mimeType)
          .putHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"")
          .putHeader(HttpHeaders.TRANSFER_ENCODING, "chunked")
          .sendFile(absolutePath);
    } else {
      log.warn("Resource not available, moving onto next handler");
      routingContext.next();
    }
  }
}
