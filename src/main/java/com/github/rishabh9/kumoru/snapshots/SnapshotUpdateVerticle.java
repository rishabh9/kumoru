package com.github.rishabh9.kumoru.snapshots;

import com.github.rishabh9.kumoru.KumoruCommon;
import com.github.rishabh9.kumoru.parser.ArtifactMetadata;
import com.github.rishabh9.kumoru.parser.KumoruAsyncXmlParser;
import com.github.rishabh9.kumoru.parser.SnapshotMetadata;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import lombok.extern.log4j.Log4j2;

import javax.xml.stream.XMLStreamException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.github.rishabh9.kumoru.KumoruCommon.REPO_ROOT;
import static com.github.rishabh9.kumoru.KumoruCommon.SNAPSHOT_URLS;

@Log4j2
public class SnapshotUpdateVerticle extends AbstractVerticle {

  private static final String SNAPSHOT = "-SNAPSHOT";
  private static final String HYPHEN = "-";
  private static final String SLASH = "/";
  private static final String DOT = ".";

  private final WebClient webClient;

  public SnapshotUpdateVerticle() {
    webClient = KumoruCommon.createWebClient(vertx);
  }

  @Override
  public void start(final Promise<Void> startPromise) {

    final int interval = 30;
    vertx.setPeriodic(
        TimeUnit.SECONDS.toMillis(interval),
        id -> {
          final ZonedDateTime now = ZonedDateTime.now();
          log.info("Snapshot updater started...");
          // Start from repository root folder,
          // and recursively visit each folder.
          visit(REPO_ROOT);
          log.info(
              "Snapshot updater finished. Next update at {}",
              now.plus(Duration.ofSeconds(interval)).format(DateTimeFormatter.ISO_DATE_TIME));
        });
    startPromise.complete();
  }

  private void visit(final String path) {
    vertx
        .fileSystem()
        .readDir(
            path,
            readDirResult -> {
              final List<String> filesAndDirectories = readDirResult.result();
              if (readDirResult.succeeded() && null != filesAndDirectories && !filesAndDirectories.isEmpty()) {
                for (String fileOrDirectory : filesAndDirectories) {
                  log.trace("Visited {}", fileOrDirectory);
                  visit(fileOrDirectory);
                  if (fileOrDirectory.endsWith(SNAPSHOT)) {
                    // Since filename ends with "-SNAPSHOT", it's a directory we need to update
                    log.debug("Processing directory {}", fileOrDirectory);
                    final SnapshotUpdateDto dto = new SnapshotUpdateDto(fileOrDirectory);
                    nextMirror(SNAPSHOT_URLS.iterator(), dto);
                  }
                }
                log.trace("");
              }
            });
  }

  private void nextMirror(final Iterator<String> iterator, final SnapshotUpdateDto dto) {
    if (iterator.hasNext()) {
      final String mirror = iterator.next();
      final String uri = mirror + dto.getMetadataXmlUriPath();
      webClient
          .getAbs(uri)
          .expect(ResponsePredicate.SC_SUCCESS)
          .as(BodyCodec.buffer())
          .send(
              asyncWebResult -> {
                final HttpResponse<Buffer> result = asyncWebResult.result();
                if (asyncWebResult.succeeded() && null != result && null != result.body()) {
                  log.debug("Found metadata on {}", uri);
                  if (isSnapshotUpdated(asyncWebResult)) {
                    log.debug("New snapshot available from {}", uri);
                    dto.setMirror(mirror);
                    updateSnapshot(dto, result.body());
                  }
                } else {
                  log.debug("Unable to retrieve metadata from {}", uri, asyncWebResult.cause());
                  nextMirror(iterator, dto);
                }
              });
    }
  }

  private void updateSnapshot(final SnapshotUpdateDto dto, final Buffer buffer) {
    vertx
        .fileSystem()
        .writeFile(
            dto.getMetadataXmlFileSystemPath(),
            buffer,
            writeResult -> {
              if (writeResult.succeeded()) {
                log.debug("Updated {}", dto.getMetadataXmlFileSystemPath());
                // Now parse the metadata.xml to extract the artifacts to download
                final Optional<SnapshotMetadata> maybeMetadata = parseMetadata(buffer);
                if (maybeMetadata.isPresent()) {
                  log.debug("Successfully parsed metadata.xml");
                  // Download all files from the list
                  final String artifactId = maybeMetadata.get().getArtifactId();
                  maybeMetadata
                      .get()
                      .getArtifactsMetadata()
                      .forEach(
                          artifactMetadata -> {
                            downloadArtifact(
                                dto.getMirror(),
                                dto.getSnapshotPath(),
                                artifactId,
                                artifactMetadata);
                          });
                }
                // Delete the contents older than last update
              } else {
                log.error("Error updating {}", dto.getMetadataXmlFileSystemPath());
              }
            });
  }

  private void downloadArtifact(
      final String mirror,
      final String path,
      final String artifactId,
      final ArtifactMetadata artifactMetadata) {

    final String absoluteUri = buildUrl(mirror, path, artifactId, artifactMetadata);
    webClient
        .getAbs(absoluteUri)
        .expect(ResponsePredicate.SC_SUCCESS)
        .as(BodyCodec.buffer())
        .send(
            asyncWebResult -> {
              if (asyncWebResult.succeeded() && null != asyncWebResult.result()) {
                final String file = buildPath(path, artifactId, artifactMetadata);
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

  private String buildPath(
      final String path, final String artifactId, final ArtifactMetadata artifactMetadata) {

    final StringBuilder builder = new StringBuilder().append(REPO_ROOT);
    buildSuffix(path, artifactId, artifactMetadata, builder);
    return builder.toString();
  }

  private void buildSuffix(
      final String path,
      final String artifactId,
      final ArtifactMetadata artifactMetadata,
      final StringBuilder builder) {

    builder
        .append(path)
        .append(SLASH)
        .append(artifactId)
        .append(HYPHEN)
        .append(artifactMetadata.getValue());

    if (null != artifactMetadata.getClassifier() && !"".equals(artifactMetadata.getClassifier())) {
      builder.append(HYPHEN).append(artifactMetadata.getClassifier());
    }

    builder.append(DOT).append(artifactMetadata.getExtension());
  }

  private String buildUrl(
      final String mirror,
      final String path,
      final String artifactId,
      final ArtifactMetadata artifactMetadata) {

    final StringBuilder builder = new StringBuilder().append(mirror);
    buildSuffix(path, artifactId, artifactMetadata, builder);
    return builder.toString();
  }

  private boolean isSnapshotUpdated(final AsyncResult<HttpResponse<Buffer>> asyncWebResult) {
    return !FileComparator.compare(asyncWebResult.result().body().getBytes(), null);
  }

  private Optional<SnapshotMetadata> parseMetadata(final Buffer buffer) {
    log.debug("Parsing metadata.xml...");
    SnapshotMetadata metadata = null;
    try {
      metadata = KumoruAsyncXmlParser.parse(buffer);
    } catch (XMLStreamException e) {
      // Stop processing it further
      log.fatal("There was an error processing the downloaded metadata.xml", e);
    }
    return Optional.ofNullable(metadata);
  }
}
