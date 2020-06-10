package com.github.rishabh9.kumoru.web.handlers;

import static com.github.rishabh9.kumoru.common.KumoruCommon.JCENTER_RELEASE_URL;
import static com.github.rishabh9.kumoru.common.KumoruCommon.JCENTER_SNAPSHOT_URL;

import io.vertx.core.Vertx;

public class JCenterMirrorHandler extends AbstractMirrorHandler {

  public JCenterMirrorHandler(final Vertx vertx) {
    super(vertx, JCENTER_RELEASE_URL, JCENTER_SNAPSHOT_URL);
  }
}