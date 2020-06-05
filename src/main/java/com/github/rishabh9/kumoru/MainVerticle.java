package com.github.rishabh9.kumoru;

import com.github.rishabh9.kumoru.snapshots.ArtifactDownloader;
import com.github.rishabh9.kumoru.snapshots.SnapshotUpdateChecker;
import com.github.rishabh9.kumoru.web.WebServer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(final Promise<Void> startPromise) {
    log.debug("Starting main verticle to deploy other verticles...");
    CompositeFuture.all(
            deployWebServer(), deploySnapshotUpdateChecker(), deployArtifactDownloader())
        .onSuccess(
            success -> {
              log.debug("Successfully deployed all verticles");
              startPromise.complete();
            })
        .onFailure(
            failure -> {
              log.error("Failed to deploy one or more verticles", failure.getCause());
              startPromise.fail(failure.getCause());
            });
  }

  private Future<Void> deployWebServer() {
    // Deploy Web server verticle
    final int processors = Runtime.getRuntime().availableProcessors();
    final DeploymentOptions webServerOptions = new DeploymentOptions();
    webServerOptions.setInstances(processors * 2);
    return deploy(new WebServer(), webServerOptions);
  }

  private Future<Void> deploySnapshotUpdateChecker() {
    // Deploy snapshot update verticle
    return deploy(new SnapshotUpdateChecker(), new DeploymentOptions());
  }

  private Future<Void> deployArtifactDownloader() {
    // Deploy verticle to help snapshot updates
    final int processors = Runtime.getRuntime().availableProcessors();
    final DeploymentOptions downloaderOptions = new DeploymentOptions();
    downloaderOptions.setInstances(processors);
    return deploy(new ArtifactDownloader(), downloaderOptions);
  }

  private Future<Void> deploy(final Verticle verticle, final DeploymentOptions deploymentOptions) {
    final Promise<Void> promise = Promise.promise();
    vertx.deployVerticle(
        verticle,
        deploymentOptions,
        deploymentResult -> {
          if (deploymentResult.succeeded()) {
            log.info("Verticle deployed - {}", deploymentResult.result());
            promise.complete();
          } else {
            log.fatal("Verticle deployment failed", deploymentResult.cause());
            promise.fail(deploymentResult.cause());
          }
        });
    return promise.future();
  }
}
