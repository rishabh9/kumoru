package com.github.rishabh9.kumoru.snapshots;

import com.github.rishabh9.kumoru.snapshots.parser.ArtifactMetadata;
import java.io.Serializable;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateMessage implements Serializable {

  private String snapshotPath;
  private String mirror;
  private String artifactId;
  private ArtifactMetadata artifactMetadata;
}
