package com.funding.velocity.config;

import java.util.Map;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
@ConfigurationProperties("funding.schemas")
public class SchemaConfig {

  private Map<String, String> schemas;
}
