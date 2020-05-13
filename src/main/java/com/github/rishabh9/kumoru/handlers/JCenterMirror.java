package com.github.rishabh9.kumoru.handlers;

import io.vertx.core.Vertx;

public class JCenterMirror extends AbstractMirror {

  private static final String RELEASE_URL = "https://jcenter.bintray.com";

  // Use the following format if you want to specify a custom JCenter user's repository
  // https://dl.bintray.com/<username>/<reponame>

  private static final String SNAPSHOT_URL = "https://oss.jfrog.org/artifactory/oss-snapshot-local";

  public JCenterMirror(final Vertx vertx) {
    super(vertx, RELEASE_URL, SNAPSHOT_URL);
  }
}
