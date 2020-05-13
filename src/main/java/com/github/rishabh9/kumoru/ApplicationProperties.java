package com.github.rishabh9.kumoru;

import java.io.IOException;
import java.util.Properties;

public enum ApplicationProperties {
  INSTANCE;

  private final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger(ApplicationProperties.class);

  private final Properties properties;

  ApplicationProperties() {
    properties = new Properties();
    try {
      properties.load(getClass().getClassLoader().getResourceAsStream("version.properties"));
    } catch (IOException e) {
      log.info(e.getMessage(), e);
    }
  }

  public String getVersion() {
    return properties.getProperty("version");
  }
}
