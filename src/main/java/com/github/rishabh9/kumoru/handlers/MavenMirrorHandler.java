package com.github.rishabh9.kumoru.handlers;

import io.vertx.core.Vertx;

public class MavenMirrorHandler extends AbstractMirrorHandler {

  private static final String RELEASE_URL = "repo.maven.apache.org/maven2";
  private static final String SNAPSHOT_URL = "oss.sonatype.org/content/repositories/snapshots";

  public MavenMirrorHandler(final Vertx vertx) {
    super(vertx, RELEASE_URL, SNAPSHOT_URL);
  }
}
