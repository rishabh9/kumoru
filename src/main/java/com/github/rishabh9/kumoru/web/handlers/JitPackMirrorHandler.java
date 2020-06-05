package com.github.rishabh9.kumoru.web.handlers;

import static com.github.rishabh9.kumoru.common.KumoruCommon.JITPACK_RELEASE_URL;

import io.vertx.core.Vertx;

public class JitPackMirrorHandler extends AbstractMirrorHandler {

  // Jitpack does not have a separate snapshot repository url.
  // The release URL is valid for both.
  public JitPackMirrorHandler(final Vertx vertx) {
    super(vertx, JITPACK_RELEASE_URL, JITPACK_RELEASE_URL);
  }
}
