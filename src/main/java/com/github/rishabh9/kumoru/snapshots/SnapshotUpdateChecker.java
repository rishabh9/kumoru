package com.github.rishabh9.kumoru.snapshots;

import static com.github.rishabh9.kumoru.common.KumoruCommon.ARTIFACT_VERTICLE;
import static com.github.rishabh9.kumoru.common.KumoruCommon.REPO_ROOT;

import com.github.rishabh9.kumoru.common.KumoruCommon;
import com.github.rishabh9.kumoru.common.KumoruConfig;
import com.github.rishabh9.kumoru.common.dto.Repositories;
import com.github.rishabh9.kumoru.common.dto.Repository;
import com.github.rishabh9.kumoru.snapshots.parser.MetadataAsyncXmlParser;
import com.github.rishabh9.kumoru.snapshots.parser.SnapshotMetadata;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.xml.stream.XMLStreamException;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class SnapshotUpdateChecker extends AbstractVerticle {

  private static final String METADATA_XML = "/maven-metadata.xml";
  private static final String SNAPSHOT = "-SNAPSHOT";
  private static long timerId;
  private static WebClient webClient;
  private static Set<String> snapshots;

  @Override
  public void start(final Promise<Void> startPromise) {
    log.debug("Starting snapshot updater");
    final Repositories repositories = KumoruConfig.INSTANCE.getRepositories();
    if (null == repositories) {
      log.warn("Repositories are not configured. Not starting snapshot updater.");
    } else {
      final Set<Repository> snapshotRepositories = repositories.getSnapshotRepositories();
      if (null == snapshotRepositories || snapshotRepositories.isEmpty()) {
        log.warn("Snapshot repositories are not configured. Not starting snapshot updater.");
      } else {
        snapshots =
            snapshotRepositories.stream().map(Repository::getUrl).collect(Collectors.toSet());
        webClient = KumoruCommon.createWebClient(vertx);
        vertx.eventBus().registerDefaultCodec(UpdateMessage.class, new UpdateMessageCodec());
        final int interval = 12;
        timerId =
            vertx.setPeriodic(
                TimeUnit.HOURS.toMillis(interval),
                id -> {
                  final ZonedDateTime now = ZonedDateTime.now();
                  log.debug("Snapshot update checker started...");
                  // Start from repository root folder,
                  // and recursively visit each folder.
                  visit(REPO_ROOT);
                  log.debug(
                      "Next snapshot update check at {}",
                      now.plus(Duration.ofSeconds(interval))
                          .format(DateTimeFormatter.ISO_DATE_TIME));
                });
        log.debug("Snapshot updater started with timer-id {}", timerId);
      }
    }
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
                    nextMirror(snapshots.iterator(), fileOrDirectory);
                  }
                }
                log.trace("");
              }
            });
  }

  private void nextMirror(final Iterator<String> iterator, final String fileOrDirectory) {
    if (iterator.hasNext()) {
      final String mirror = iterator.next();
      final String snapshotPath = fileOrDirectory.substring(REPO_ROOT.length());
      final String uri = mirror + snapshotPath + METADATA_XML;
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
                  final String metadataXmlFileSystemPath = fileOrDirectory + METADATA_XML;
                  if (isSnapshotUpdated(downloadedFile, metadataXmlFileSystemPath)) {
                    log.debug("New snapshot available from {}", uri);
                    deleteOldSnapshots(fileOrDirectory)
                        .onComplete(
                            asyncResult ->
                                // Update after attempting to delete is complete
                                updateSnapshot(
                                    metadataXmlFileSystemPath,
                                    snapshotPath,
                                    mirror,
                                    result.body()));
                  }
                } else {
                  log.debug("Unable to retrieve metadata from {}", uri, asyncWebResult.cause());
                  nextMirror(iterator, fileOrDirectory);
                }
              });
    }
  }

  private Future<Void> deleteOldSnapshots(final String directory) {
    log.debug("Deleting old snapshots from {}", directory);
    final Promise<Void> promise = Promise.promise();
    vertx
        .fileSystem()
        .readDir(
            directory,
            readResult -> {
              if (readResult.succeeded()) {
                CompositeFuture.all(
                        readResult.result().stream().map(this::delete).collect(Collectors.toList()))
                    .onSuccess(success -> promise.complete())
                    .onFailure(
                        failure -> {
                          log.error("Error deleting all files", failure.getCause());
                          promise.fail(failure.getCause());
                        });
              } else {
                log.error("Error listing files", readResult.cause());
                promise.fail(readResult.cause());
              }
            });
    return promise.future();
  }

  private Future<Void> delete(final String fileOrDirectory) {
    log.debug("Deleting {}", fileOrDirectory);
    final Promise<Void> promise = Promise.promise();
    vertx
        .fileSystem()
        .deleteRecursive(
            fileOrDirectory,
            true,
            deleteResult -> {
              if (deleteResult.failed()) {
                log.error("Unable to delete {}", fileOrDirectory, deleteResult.cause());
                promise.fail(deleteResult.cause());
              }
              promise.complete();
            });
    return promise.future();
  }

  private void updateSnapshot(
      final String metadataXmlFileSystemPath,
      final String snapshotPath,
      final String mirror,
      final Buffer buffer) {
    vertx
        .fileSystem()
        .writeFile(
            metadataXmlFileSystemPath,
            buffer,
            writeResult -> {
              if (writeResult.succeeded()) {
                log.debug("Updated {}", metadataXmlFileSystemPath);
                // Now parse the metadata.xml to extract the artifacts to download
                final Optional<SnapshotMetadata> maybeMetadata = parseMetadata(buffer);
                if (maybeMetadata.isPresent()) {
                  log.debug("Successfully parsed metadata.xml");
                  // Download all files from the list
                  maybeMetadata
                      .get()
                      .getArtifactsMetadata()
                      .forEach(
                          artifactMetadata -> {
                            log.debug("Processing artifact {}", artifactMetadata);
                            final UpdateMessage message =
                                UpdateMessage.builder()
                                    .artifactId(maybeMetadata.get().getArtifactId())
                                    .artifactMetadata(artifactMetadata)
                                    .mirror(mirror)
                                    .snapshotPath(snapshotPath)
                                    .build();
                            final DeliveryOptions deliveryOptions = new DeliveryOptions();
                            vertx.eventBus().send(ARTIFACT_VERTICLE, message, deliveryOptions);
                          });
                }
              } else {
                log.error("Error updating {}", metadataXmlFileSystemPath, writeResult.cause());
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
