package com.github.rishabh9.kumoru.handlers;

import io.vertx.core.Vertx;

public class JitPackMirrorHandler extends AbstractMirrorHandler {

  private static final String RELEASE_URL = "jitpack.io";

  // Jitpack does not have a separate snapshot repository url.
  // The release URL is valid for both.
  public JitPackMirrorHandler(final Vertx vertx) {
    super(vertx, RELEASE_URL, RELEASE_URL);
  }
}
