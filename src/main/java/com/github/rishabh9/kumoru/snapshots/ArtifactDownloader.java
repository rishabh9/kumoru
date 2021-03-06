package com.github.rishabh9.kumoru.snapshots;

import static com.github.rishabh9.kumoru.common.KumoruCommon.ARTIFACT_VERTICLE;
import static com.github.rishabh9.kumoru.common.KumoruCommon.REPO_ROOT;

import com.github.rishabh9.kumoru.common.KumoruCommon;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ArtifactDownloader extends AbstractVerticle {

  private static final String HYPHEN = "-";
  private static final String SLASH = "/";
  private static final String DOT = ".";

  private WebClient webClient;
  private MessageConsumer<UpdateMessage> messageConsumer;

  @Override
  public void start(final Promise<Void> startPromise) {
    webClient = KumoruCommon.createWebClient(vertx);
    messageConsumer =
        vertx
            .eventBus()
            .consumer(
                ARTIFACT_VERTICLE,
                message -> {
                  final UpdateMessage dto = message.body();
                  log.debug("Message received... {}", dto);
                  downloadArtifact(dto);
                });
    log.debug("Artifact downloader started");
    startPromise.complete();
  }

  @Override
  public void stop(final Promise<Void> stopPromise) {
    if (null != messageConsumer && messageConsumer.isRegistered()) {
      log.debug("Un-registering consumer...");
      messageConsumer.unregister(
          unregisterResult -> {
            if (unregisterResult.succeeded()) {
              log.debug("Consumer un-registered");
              if (null != webClient) {
                log.debug("Closing web client...");
                webClient.close();
              }
              stopPromise.complete();
            } else {
              log.fatal("Unable to un-register consumer", unregisterResult.cause());
              stopPromise.fail(unregisterResult.cause());
            }
          });
    } else {
      stopPromise.complete();
    }
  }

  private void downloadArtifact(final UpdateMessage dto) {

    final String absoluteUri = buildUrl(dto);
    log.debug("Downloading {}", absoluteUri);
    webClient
        .getAbs(absoluteUri)
        .expect(ResponsePredicate.SC_SUCCESS)
        .as(BodyCodec.buffer())
        .send(
            asyncWebResult -> {
              if (asyncWebResult.succeeded() && null != asyncWebResult.result()) {
                final String file = buildPath(dto);
                vertx
                    .fileSystem()
                    .writeFile(
                        file,
                        asyncWebResult.result().body(),
                        writeResult -> {
                          if (writeResult.succeeded()) {
                            log.debug("Saved file {}", file);
                          } else {
                            log.error("Error saving {}", file, writeResult.cause());
                          }
                        });
              } else {
                log.error("Error downloading {}", absoluteUri, asyncWebResult.cause());
              }
            });
  }

  private String buildPath(final UpdateMessage dto) {

    final StringBuilder builder = new StringBuilder().append(REPO_ROOT);
    buildSuffix(dto, builder);
    return builder.toString();
  }

  private String buildUrl(final UpdateMessage dto) {

    final StringBuilder builder = new StringBuilder().append(dto.getMirror());
    buildSuffix(dto, builder);
    return builder.toString();
  }

  private void buildSuffix(final UpdateMessage dto, final StringBuilder builder) {

    builder
        .append(dto.getSnapshotPath())
        .append(SLASH)
        .append(dto.getArtifactId())
        .append(HYPHEN)
        .append(dto.getArtifactMetadata().getValue());

    if (null != dto.getArtifactMetadata().getClassifier()
        && !"".equals(dto.getArtifactMetadata().getClassifier())) {
      builder.append(HYPHEN).append(dto.getArtifactMetadata().getClassifier());
    }

    builder.append(DOT).append(dto.getArtifactMetadata().getExtension());
  }
}
