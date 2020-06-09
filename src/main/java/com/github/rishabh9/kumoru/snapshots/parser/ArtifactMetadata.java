package com.github.rishabh9.kumoru.snapshots.parser;

import java.io.Serializable;
import lombok.Data;

@Data
public class ArtifactMetadata implements Serializable {
  private String classifier;
  private String extension;
  private String value;
  private String updated;
}
