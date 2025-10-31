package com.funding.velocity.config;

import java.util.Map;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("funding.json-validation")
public class SchemaConfig {

  private Map<String, String> schemas;
}
