package com.github.rishabh9.kumoru.common.dto;

import java.util.Set;
import lombok.Data;

@Data
public class Repositories {
  private Set<Repository> repositories;
  private Set<Repository> snapshotRepositories;
}
