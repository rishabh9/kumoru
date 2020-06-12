package com.github.rishabh9.kumoru.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rishabh9.kumoru.common.dto.Repositories;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.InputStream;

@Log4j2
public final class KumoruConfig {

  public static final KumoruConfig INSTANCE = new KumoruConfig();

  @Getter private final int bodyLimit;
  @Getter private final boolean enableAccessLog;
  @Getter private final int kumoruPort;
  @Getter private final Repositories repositories;

  private KumoruConfig() {
    bodyLimit = getLimit();
    enableAccessLog = isAccessLogEnabled();
    kumoruPort = getPort();
    repositories = readRepositories();
  }

  /**
   * Fetch the configured limit for uploads.
   *
   * @return Body limit
   */
  private int getLimit() {
    // Body limited to 50MB
    final int defaultBodyLimit = 50000000;
    final String kumoruBodyLimit = System.getenv("KUMORU_BODY_LIMIT");
    log.debug("Body limit configured as {} bytes", kumoruBodyLimit);
    if (null != kumoruBodyLimit
        && !kumoruBodyLimit.isEmpty()
        && !kumoruBodyLimit.matches(".*\\D.*")) {
      return Integer.parseInt(kumoruBodyLimit);
    } else {
      log.debug("Setting body limit as {} bytes", defaultBodyLimit);
      return defaultBodyLimit;
    }
  }

  /**
   * Fetch the configured value for access logs.
   *
   * @return TRUE if access logs are enabled, else FALSE.
   */
  private boolean isAccessLogEnabled() {
    final String flag = System.getenv("KUMORU_ACCESS_LOG");
    if (null != flag && !flag.isEmpty() && flag.matches("^(true|false)$")) {
      return Boolean.parseBoolean(flag);
    }
    return false;
  }

  /**
   * Fetch the configured value for the port.
   *
   * @return The port to listen on.
   */
  // I see no utility of this facility if deployed as a Docker container.
  private int getPort() {
    final int defaultPort = 8888;
    final String port = System.getenv("KUMORU_PORT");
    log.debug("Port configured as {}", port);
    if (null != port
        && !port.isEmpty()
        && port.matches(
            "^()([1-9]|[1-5]?[0-9]{2,4}|6[1-4][0-9]{3}|65[1-4][0-9]{2}|655[1-2][0-9]|6553[1-5])$")) {
      return Integer.parseInt(port);
    } else {
      log.debug("Setting default port {}", defaultPort);
      return defaultPort;
    }
  }

  /**
   * Reads the repositories to be configured.
   *
   * @return The future for repositories
   */
  private Repositories readRepositories() {
    try {
      final ObjectMapper mapper = new ObjectMapper();
      final InputStream stream = getClass().getResourceAsStream("/repositories.json");
      return mapper.readValue(stream, Repositories.class);
    } catch (IOException e) {
      log.error("Unable to load repositories to proxy. Serving local content only", e);
      return null;
    }
  }
}
