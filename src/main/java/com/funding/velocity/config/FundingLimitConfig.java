package com.funding.velocity.config;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties("funding.limits")
public class FundingLimitConfig {

  private Map<String, Integer> amounts;
  private Map<String, Integer> loads;

}
