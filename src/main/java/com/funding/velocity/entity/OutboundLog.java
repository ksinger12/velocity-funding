package com.funding.velocity.entity;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Table("outbound_log")
public class OutboundLog {

  @Id
  private Long id;
  private String traceId;
  private boolean wasSuccessful;
}
