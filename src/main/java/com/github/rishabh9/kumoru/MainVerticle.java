package com.github.rishabh9.kumoru;

import com.github.rishabh9.kumoru.common.KumoruConfig;
import com.github.rishabh9.kumoru.common.VersionProperties;
import com.github.rishabh9.kumoru.snapshots.ArtifactDownloader;
import com.github.rishabh9.kumoru.snapshots.SnapshotUpdateChecker;
import com.github.rishabh9.kumoru.web.WebServer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
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
              log.info(
                  "Kumoru server [v{}] is up and running on port {}",
                  VersionProperties.INSTANCE.getVersion(),
                  KumoruConfig.INSTANCE.getKumoruPort());
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
    return deploy(WebServer.class.getName(), webServerOptions);
  }

  private Future<String> deploySnapshotUpdateChecker() {
    // Deploy snapshot update verticle
    return deploy(SnapshotUpdateChecker.class.getName(), new DeploymentOptions());
  }

  private Future<String> deployArtifactDownloader() {
    // Deploy verticle to help snapshot updates
    // final int processors = Runtime.getRuntime().availableProcessors();
    final DeploymentOptions downloaderOptions = new DeploymentOptions();
    // .setInstances(processors)
    // .setWorkerPoolName("artifact-download-pool")
    // .setWorkerPoolSize(processors)
    // .setWorker(true);
    return deploy(ArtifactDownloader.class.getName(), downloaderOptions);
  }

  private Future<String> deploy(
      final String verticleName, final DeploymentOptions deploymentOptions) {
    final Promise<String> promise = Promise.promise();
    vertx.deployVerticle(
        verticleName,
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
