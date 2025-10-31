package com.funding.velocity.entity;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Builder
@Table("customer_transaction")
public class CustomerTransaction {

  @Id
  private Long id;
  private String traceId;
  private String requestId;
  private String customerId;
  private Double loadAmount;
  private LocalDateTime time;

}
