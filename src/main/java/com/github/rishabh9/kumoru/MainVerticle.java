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
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class MainVerticle extends AbstractVerticle {

  private static List<String> deploymentIds;

  @Override
  public void start(final Promise<Void> startPromise) {
    log.debug("Starting main verticle to deploy other verticles...");
    CompositeFuture.all(
            deployWebServer(), deploySnapshotUpdateChecker(), deployArtifactDownloader())
        .onSuccess(
            success -> {
              log.debug("Successfully deployed all verticles");
              deploymentIds = success.list();
              startPromise.complete();
            })
        .onFailure(
            failure -> {
              log.fatal("Failed to deploy one or more verticles", failure.getCause());
              startPromise.fail(failure.getCause());
            });
  }

  private Future<String> deployWebServer() {
    // Deploy Web server verticle
    final int processors = Runtime.getRuntime().availableProcessors();
    final DeploymentOptions webServerOptions = new DeploymentOptions();
    webServerOptions.setInstances(processors * 2);
    return deploy(new WebServer(), webServerOptions);
  }

  private Future<String> deploySnapshotUpdateChecker() {
    // Deploy snapshot update verticle
    return deploy(new SnapshotUpdateChecker(), new DeploymentOptions());
  }

  private Future<String> deployArtifactDownloader() {
    // Deploy verticle to help snapshot updates
    final int processors = Runtime.getRuntime().availableProcessors();
    final DeploymentOptions downloaderOptions = new DeploymentOptions();
    downloaderOptions.setInstances(processors);
    return deploy(new ArtifactDownloader(), downloaderOptions);
  }

  private Future<String> deploy(
      final Verticle verticle, final DeploymentOptions deploymentOptions) {
    final Promise<String> promise = Promise.promise();
    vertx.deployVerticle(
        verticle,
        deploymentOptions,
        deploymentResult -> {
          if (deploymentResult.succeeded()) {
            log.debug("Verticle deployed - {}", deploymentResult.result());
            promise.complete(deploymentResult.result());
          } else {
            log.fatal("Verticle deployment failed", deploymentResult.cause());
            promise.fail(deploymentResult.cause());
          }
        });
    return promise.future();
  }

  @Override
  public void stop(final Promise<Void> stopPromise) {
    if (null != deploymentIds) {
      log.debug("Un-deploying {} deployments", deploymentIds.size());
      CompositeFuture.all(deploymentIds.stream().map(this::unDeploy).collect(Collectors.toList()))
          .onSuccess(
              success -> {
                log.debug("Successfully un-deployed all verticles");
                stopPromise.complete();
              })
          .onFailure(
              failure -> {
                log.fatal("Failed to un-deploy one or more verticles", failure.getCause());
                stopPromise.fail(failure.getCause());
              });
    }
  }

  private Future<Void> unDeploy(final String deploymentId) {
    final Promise<Void> promise = Promise.promise();
    vertx.undeploy(
        deploymentId,
        unDeployResult -> {
          if (unDeployResult.succeeded()) {
            log.debug("Verticle un-deployed - {}", deploymentId);
            promise.complete();
          } else {
            log.fatal("Verticle failed to un-deploy - {}", deploymentId, unDeployResult.cause());
            promise.fail(unDeployResult.cause());
          }
        });
    return promise.future();
  }
}
