package com.github.rishabh9.kumoru.handlers;

import io.vertx.core.Vertx;

public class MavenMirror extends AbstractMirror {

  private static final String RELEASE_URL = "https://repo.maven.apache.org/maven2";

  private static final String SNAPSHOT_URL =
      "https://oss.sonatype.org/content/repositories/snapshots";

  public MavenMirror(final Vertx vertx) {
    super(vertx, RELEASE_URL, SNAPSHOT_URL);
  }
}
