package com.funding.velocity.entity;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Table("inbound_log")
public class InboundLog {

  @Id
  private Long id;
  private String traceId;
  private String customerId;
  private String payload;

}
