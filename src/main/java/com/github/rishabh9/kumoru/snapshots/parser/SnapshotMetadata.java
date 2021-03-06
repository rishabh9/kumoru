package com.github.rishabh9.kumoru.snapshots.parser;

import java.util.Set;
import lombok.Data;

@Data
public class SnapshotMetadata {

  private String artifactId;
  private Set<ArtifactMetadata> artifactsMetadata;
}
