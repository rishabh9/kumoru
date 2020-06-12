package com.github.rishabh9.kumoru.common.dto;

import lombok.Data;

@Data
public class Repository {
  private String name;
  private String url;
  private BasicAuth basicAuth;
  private BearerToken bearerToken;
}
