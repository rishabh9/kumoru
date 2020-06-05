package com.github.rishabh9.kumoru.snapshots;

import static com.github.rishabh9.kumoru.common.KumoruCommon.REPO_ROOT;

import com.github.rishabh9.kumoru.snapshots.parser.ArtifactMetadata;
import lombok.Getter;
import lombok.Setter;

public class UpdateMessage {

  private static final String METADATA_XML = "/maven-metadata.xml";

  @Getter private final String directory;
  @Getter private final String snapshotPath;
  @Getter private final String metadataXmlUriPath;
  @Getter private final String metadataXmlFileSystemPath;
  @Getter @Setter private String mirror;
  @Getter @Setter private String artifactId;
  @Getter @Setter private ArtifactMetadata artifactMetadata;

  /**
   * Constructor.
   *
   * @param directory The SNAPSHOT directory to update
   */
  UpdateMessage(final String directory) {
    this.directory = directory;
    // Remove the ROOT directory prefix to obtain the URL path for the snapshot
    this.snapshotPath = directory.substring(REPO_ROOT.length());
    // Setup the URL path to metadata.xml
    this.metadataXmlUriPath = this.snapshotPath + METADATA_XML;
    this.metadataXmlFileSystemPath = this.directory + METADATA_XML;
  }
}
