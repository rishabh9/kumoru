package com.github.rishabh9.kumoru.handlers;

import io.vertx.core.Vertx;

public class JCenterMirrorHandler extends AbstractMirrorHandler {

  private static final String RELEASE_URL = "jcenter.bintray.com";

  // Use the following format if you want to specify a custom JCenter user's repository
  // https://dl.bintray.com/<username>/<reponame>

  private static final String SNAPSHOT_URL = "oss.jfrog.org/artifactory/oss-snapshot-local";

  public JCenterMirrorHandler(final Vertx vertx) {
    super(vertx, RELEASE_URL, SNAPSHOT_URL);
  }
}
