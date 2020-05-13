package com.github.rishabh9.kumoru;

import java.io.IOException;
import java.util.Properties;

public enum VersionProperties {
  INSTANCE;

  private final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger(VersionProperties.class);

  private final Properties properties;

  VersionProperties() {
    properties = new Properties();
    try {
      properties.load(getClass().getClassLoader().getResourceAsStream("version.properties"));
    } catch (IOException e) {
      log.warn(e.getMessage(), e);
    }
  }

  public String getVersion() {
    return properties.getProperty("version");
  }
}
