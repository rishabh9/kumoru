package com.github.rishabh9.kumoru.handlers;

import static com.github.rishabh9.kumoru.KumoruCommon.MAVEN_RELEASE_URL;
import static com.github.rishabh9.kumoru.KumoruCommon.MAVEN_SNAPSHOT_URL;

import io.vertx.core.Vertx;

public class MavenMirrorHandler extends AbstractMirrorHandler {

  public MavenMirrorHandler(final Vertx vertx) {
    super(vertx, MAVEN_RELEASE_URL, MAVEN_SNAPSHOT_URL);
  }
}
