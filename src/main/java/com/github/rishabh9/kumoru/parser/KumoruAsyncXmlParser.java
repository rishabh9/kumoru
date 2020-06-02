package com.github.rishabh9.kumoru.parser;

import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import io.vertx.core.buffer.Buffer;

import javax.xml.stream.XMLStreamException;
import java.util.HashSet;

public final class KumoruAsyncXmlParser {

  private static final String SNAPSHOT_VERSIONS = "snapshotVersions";
  private static final String SNAPSHOT_VERSION = "snapshotVersion";
  private static final String VERSIONING = "versioning";
  private static final String METADATA = "metadata";
  private static final String CLASSIFIER = "classifier";
  private static final String EXTENSION = "extension";
  private static final String VALUE = "value";
  private static final String UPDATED = "updated";
  private static final String ARTIFACT_ID = "artifactId";

  /**
   * Parse the maven-metadata.xml and extract the files to be downloaded.
   *
   * @param buffer The buffer to parse
   * @return Parsed metadata of the snapshot.
   * @throws XMLStreamException Parsing exception
   */
  public static SnapshotMetadata parse(final Buffer buffer) throws XMLStreamException {
    // Setup factory
    final AsyncXMLInputFactory factory = new InputFactoryImpl();
    final AsyncXMLStreamReader<AsyncByteArrayFeeder> parser =
        factory.createAsyncFor(buffer.getBytes());

    final SnapshotMetadata meta = new SnapshotMetadata();

    while (parser.hasNext()) {
      parser.next();

      extractArtifactId(parser, meta);
      parseVersioning(parser, meta);

      // We have reached the end of the metadata.xml
      if (parser.isEndElement() && parser.getLocalName().equalsIgnoreCase(METADATA)) {
        parser.getInputFeeder().endOfInput();
        break;
      }
    }

    // Close parser
    parser.close();
    return meta;
  }

  private static void parseVersioning(
      final AsyncXMLStreamReader<AsyncByteArrayFeeder> parser, final SnapshotMetadata meta)
      throws XMLStreamException {

    if (parser.isStartElement() && parser.getLocalName().equalsIgnoreCase(VERSIONING)) {
      while (true) {
        // Do nothing until you reach snapshotVersions
        parser.next();

        if (parser.isStartElement() && parser.getLocalName().equalsIgnoreCase(SNAPSHOT_VERSIONS)) {
          meta.setArtifactsMetadata(new HashSet<>());
          extractArtifactsMetadata(parser, meta);
          // We have extracted individual snapshot versions, so break from while loop
          break;
        }
      }
    }
  }

  private static void extractArtifactsMetadata(
      final AsyncXMLStreamReader<AsyncByteArrayFeeder> parser, final SnapshotMetadata meta)
      throws XMLStreamException {

    while (true) {
      parser.next();

      if (parser.isStartElement() && parser.getLocalName().equalsIgnoreCase(SNAPSHOT_VERSION)) {
        // Start parsing every listed artifact
        final ArtifactMetadata artifactMetadata = new ArtifactMetadata();
        meta.getArtifactsMetadata().add(artifactMetadata);

        while (true) {
          parser.next();

          extractArtifactMetadata(parser, artifactMetadata);

          if (parser.isEndElement() && parser.getLocalName().equalsIgnoreCase(SNAPSHOT_VERSION)) {
            // Parsed this snapshotVersion, so stop the loop
            break;
          }
        }

      } else if (parser.isEndElement()
          && parser.getLocalName().equalsIgnoreCase(SNAPSHOT_VERSIONS)) {
        // We have processed all artifacts.
        break;
      }
    }
  }

  private static void extractArtifactMetadata(
      final AsyncXMLStreamReader<AsyncByteArrayFeeder> parser,
      final ArtifactMetadata artifactMetadata)
      throws XMLStreamException {

    extractClassifier(parser, artifactMetadata);
    extractExtension(parser, artifactMetadata);
    extractValue(parser, artifactMetadata);
    extractUpdated(parser, artifactMetadata);
  }

  private static void extractClassifier(
      final AsyncXMLStreamReader<AsyncByteArrayFeeder> parser,
      final ArtifactMetadata artifactMetadata)
      throws XMLStreamException {

    if (parser.isStartElement() && parser.getLocalName().equalsIgnoreCase(CLASSIFIER)) {
      while (true) {
        parser.next();

        if (parser.isCharacters()) {
          artifactMetadata.setClassifier(parser.getText());
          break;
        }
      }
    }
  }

  private static void extractExtension(
      final AsyncXMLStreamReader<AsyncByteArrayFeeder> parser,
      final ArtifactMetadata artifactMetadata)
      throws XMLStreamException {

    if (parser.isStartElement() && parser.getLocalName().equalsIgnoreCase(EXTENSION)) {
      while (true) {
        parser.next();

        if (parser.isCharacters()) {
          artifactMetadata.setExtension(parser.getText());
          break;
        }
      }
    }
  }

  private static void extractValue(
      final AsyncXMLStreamReader<AsyncByteArrayFeeder> parser,
      final ArtifactMetadata artifactMetadata)
      throws XMLStreamException {

    if (parser.isStartElement() && parser.getLocalName().equalsIgnoreCase(VALUE)) {
      while (true) {
        parser.next();

        if (parser.isCharacters()) {
          artifactMetadata.setValue(parser.getText());
          break;
        }
      }
    }
  }

  private static void extractUpdated(
      final AsyncXMLStreamReader<AsyncByteArrayFeeder> parser,
      final ArtifactMetadata artifactMetadata)
      throws XMLStreamException {

    if (parser.isStartElement() && parser.getLocalName().equalsIgnoreCase(UPDATED)) {
      while (true) {
        parser.next();

        if (parser.isCharacters()) {
          artifactMetadata.setUpdated(parser.getText());
          break;
        }
      }
    }
  }

  private static void extractArtifactId(
      final AsyncXMLStreamReader<AsyncByteArrayFeeder> parser, final SnapshotMetadata meta)
      throws XMLStreamException {

    if (parser.isStartElement() && parser.getLocalName().equalsIgnoreCase(ARTIFACT_ID)) {
      while (true) {
        // Do nothing until you get the text.
        parser.next();

        if (parser.isCharacters()) {
          meta.setArtifactId(parser.getText());
          break;
        }
      }
    }
  }
}
