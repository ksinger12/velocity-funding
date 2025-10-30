package com.funding.velocity.config;

import java.util.Map;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
@ConfigurationProperties("funding.limits")
public class FundingLimitConfig {

  private Map<String, Integer> amounts;
  private Map<String, Integer> loads;

}
