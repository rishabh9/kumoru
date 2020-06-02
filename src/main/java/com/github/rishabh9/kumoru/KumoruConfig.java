package com.github.rishabh9.kumoru;

import lombok.extern.log4j.Log4j2;

@Log4j2
public final class KumoruConfig {

  /**
   * Fetch the configured limit for uploads.
   *
   * @return Body limit
   */
  public int getBodyLimit() {
    // Body limited to 50MB
    final int defaultBodyLimit = 50000000;
    final String bodyLimit = System.getenv("KUMORU_BODY_LIMIT");
    log.debug("Body limit configured as {} bytes", bodyLimit);
    if (null != bodyLimit && !bodyLimit.isEmpty() && !bodyLimit.matches(".*\\D.*")) {
      return Integer.parseInt(bodyLimit);
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
  public boolean enableAccessLog() {
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
  public int getKumoruPort() {
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
}
