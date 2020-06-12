package com.github.rishabh9.kumoru.common;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import java.util.ArrayList;
import java.util.List;

public class KumoruCommon {
  public static final String REPO_ROOT = "/srv/repo";

  public static final String MAVEN_SNAPSHOT_URL =
      "https://oss.sonatype.org/content/repositories/snapshots";
  // Use the following format if you want to specify a custom JCenter user's repository
  // https://dl.bintray.com/<username>/<reponame>
  public static final String JCENTER_SNAPSHOT_URL =
      "https://oss.jfrog.org/artifactory/oss-snapshot-local";
  public static final String JITPACK_RELEASE_URL = "https://jitpack.io";

  public static final List<String> SNAPSHOT_URLS;

  public static final String ARTIFACT_VERTICLE = "artifactVerticle";

  static {
    final int initialCapacity = 3;
    SNAPSHOT_URLS = new ArrayList<>(initialCapacity);
    SNAPSHOT_URLS.add(MAVEN_SNAPSHOT_URL);
    SNAPSHOT_URLS.add(JCENTER_SNAPSHOT_URL);
    SNAPSHOT_URLS.add(JITPACK_RELEASE_URL);
  }

  /**
   * Create a web client.
   *
   * @param vertx The vertx instance
   * @return The configured web client
   */
  public static WebClient createWebClient(final Vertx vertx) {
    final WebClientOptions webClientOptions =
        new WebClientOptions()
            .setSsl(true)
            .setUserAgent("kumoru/" + VersionProperties.INSTANCE.getVersion())
            .setFollowRedirects(true)
            .setMaxRedirects(5);
    return WebClient.create(vertx, webClientOptions);
  }
}
