package com.github.rishabh9.kumoru.snapshots;

import static com.github.rishabh9.kumoru.common.KumoruCommon.ARTIFACT_VERTICLE;
import static com.github.rishabh9.kumoru.common.KumoruCommon.REPO_ROOT;
import static com.github.rishabh9.kumoru.common.KumoruCommon.SNAPSHOT_URLS;

import com.github.rishabh9.kumoru.common.KumoruCommon;
import com.github.rishabh9.kumoru.snapshots.parser.MetadataAsyncXmlParser;
import com.github.rishabh9.kumoru.snapshots.parser.SnapshotMetadata;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.xml.stream.XMLStreamException;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class SnapshotUpdateChecker extends AbstractVerticle {

  private static final String SNAPSHOT = "-SNAPSHOT";
  private static long timerId;
  private static WebClient webClient;

  @Override
  public void start(final Promise<Void> startPromise) {
    log.debug("Starting snapshot updater");
    webClient = KumoruCommon.createWebClient(vertx);
    final int interval = 30;
    timerId =
        vertx.setPeriodic(
            TimeUnit.SECONDS.toMillis(interval),
            id -> {
              final ZonedDateTime now = ZonedDateTime.now();
              log.debug("Snapshot update checker started...");
              // Start from repository root folder,
              // and recursively visit each folder.
              visit(REPO_ROOT);
              log.debug(
                  "Next snapshot update check at {}",
                  now.plus(Duration.ofSeconds(interval)).format(DateTimeFormatter.ISO_DATE_TIME));
            });
    log.debug("Snapshot updater started with timer-id {}", timerId);
    startPromise.complete();
  }

  @Override
  public void stop(final Promise<Void> stopPromise) {
    log.debug("Stopping snapshot updater");
    log.debug("Closing web client...");
    webClient.close();
    if (timerId != 0) {
      log.debug("Cancelling timer... {}", timerId);
      vertx.cancelTimer(timerId);
    }
    stopPromise.complete();
  }

  private void visit(final String path) {
    vertx
        .fileSystem()
        .readDir(
            path,
            readDirResult -> {
              final List<String> filesAndDirectories = readDirResult.result();
              if (readDirResult.succeeded()
                  && null != filesAndDirectories
                  && !filesAndDirectories.isEmpty()) {
                for (String fileOrDirectory : filesAndDirectories) {
                  log.trace("Visited {}", fileOrDirectory);
                  visit(fileOrDirectory);
                  if (fileOrDirectory.endsWith(SNAPSHOT)) {
                    // Since filename ends with "-SNAPSHOT", it's a directory we need to update
                    log.debug("Found snapshot to update {}", fileOrDirectory);
                    final UpdateMessage message = new UpdateMessage(fileOrDirectory);
                    nextMirror(SNAPSHOT_URLS.iterator(), message);
                  }
                }
                log.trace("");
              }
            });
  }

  private void nextMirror(final Iterator<String> iterator, final UpdateMessage message) {
    if (iterator.hasNext()) {
      final String mirror = iterator.next();
      message.setMirror(mirror);
      final String uri = mirror + message.getMetadataXmlUriPath();
      webClient
          .getAbs(uri)
          .expect(ResponsePredicate.SC_SUCCESS)
          .as(BodyCodec.buffer())
          .send(
              asyncWebResult -> {
                final HttpResponse<Buffer> result = asyncWebResult.result();
                if (asyncWebResult.succeeded() && null != result && null != result.body()) {
                  log.debug("Found metadata on {}", uri);
                  final byte[] downloadedFile = asyncWebResult.result().body().getBytes();
                  if (isSnapshotUpdated(downloadedFile, message.getMetadataXmlFileSystemPath())) {
                    log.debug("New snapshot available from {}", uri);
                    updateSnapshot(message, result.body());
                  }
                } else {
                  log.debug("Unable to retrieve metadata from {}", uri, asyncWebResult.cause());
                  nextMirror(iterator, message);
                }
              });
    }
  }

  private void updateSnapshot(final UpdateMessage message, final Buffer buffer) {
    vertx
        .fileSystem()
        .writeFile(
            message.getMetadataXmlFileSystemPath(),
            buffer,
            writeResult -> {
              if (writeResult.succeeded()) {
                log.debug("Updated {}", message.getMetadataXmlFileSystemPath());
                // Now parse the metadata.xml to extract the artifacts to download
                final Optional<SnapshotMetadata> maybeMetadata = parseMetadata(buffer);
                if (maybeMetadata.isPresent()) {
                  log.debug("Successfully parsed metadata.xml");
                  // Download all files from the list
                  message.setArtifactId(maybeMetadata.get().getArtifactId());
                  maybeMetadata
                      .get()
                      .getArtifactsMetadata()
                      .forEach(
                          artifactMetadata -> {
                            message.setArtifactMetadata(artifactMetadata);
                            vertx.eventBus().publish(ARTIFACT_VERTICLE, message);
                          });
                }
              } else {
                log.error("Error updating {}", message.getMetadataXmlFileSystemPath());
              }
            });
  }

  private boolean isSnapshotUpdated(
      final byte[] downloadedFile, final String existingMetadataFile) {
    return !FileComparator.compare(downloadedFile, existingMetadataFile);
  }

  private Optional<SnapshotMetadata> parseMetadata(final Buffer buffer) {
    log.debug("Parsing metadata.xml...");
    SnapshotMetadata metadata = null;
    try {
      metadata = MetadataAsyncXmlParser.parse(buffer);
    } catch (XMLStreamException e) {
      // Stop processing it further
      log.fatal("There was an error processing the downloaded metadata.xml", e);
    }
    return Optional.ofNullable(metadata);
  }
}
