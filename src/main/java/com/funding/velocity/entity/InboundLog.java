package com.funding.velocity.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Builder
@Table("inbound_log")
public class InboundLog {

  @Id
  private Long id;
  private String traceId;
  private String path;
  private String method;
  private String payload;

}
