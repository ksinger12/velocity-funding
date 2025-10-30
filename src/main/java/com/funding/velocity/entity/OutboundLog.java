package com.funding.velocity.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Builder
@Table("outbound_log")
public class OutboundLog {

  @Id
  private Long id;
  private String traceId;
  private String customerId;
  private boolean wasSuccessful;
}
