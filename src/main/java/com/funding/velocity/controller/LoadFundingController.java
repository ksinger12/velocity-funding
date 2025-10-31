package com.funding.velocity.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.funding.velocity.config.SchemaConfig;
import com.funding.velocity.service.LoadFundingService;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class LoadFundingController {

  public final LoadFundingService loadFundingService;
  public final SchemaConfig schemaConfig;

  public LoadFundingController(LoadFundingService loadFundingService, SchemaConfig schemaConfig) {

    this.loadFundingService = loadFundingService;
    this.schemaConfig = schemaConfig;
  }

  @PostMapping(value = "/load-fund-data", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<String> loadFunds(@RequestBody String json) throws JsonProcessingException {

    JSONObject jsonObject = new JSONObject(json);
    JsonNode node = new ObjectMapper().readTree(json);

    Set<ValidationMessage> validationMessages = retrieveLoadFundsSchema()
        .map(v -> v.validate(node))
        .orElse(
            Set.of(ValidationMessage.builder()
                .message("Error retrieving schema")
                .build())
        );

    if (validationMessages.isEmpty()) {
      log.info("Attempting to fund customer: {}", jsonObject.get("customer_id"));

      JSONObject response = loadFundingService.loadFunds(jsonObject);

      return response.getBoolean("accepted")
          ? ResponseEntity.ok(response.toString())
          : ResponseEntity.badRequest().body(response.toString());
    }

    log.warn("Failed to load funds: {}. Validation errors: {}", json, validationMessages);

    return ResponseEntity.badRequest().build();
  }

  private Optional<JsonSchema> retrieveLoadFundsSchema() {

    String schemaPath = schemaConfig.getSchemas().get("LoadFunds");

    try (InputStream schemaStream = getClass().getClassLoader().getResourceAsStream(schemaPath)) {

      JsonNode schemaNode = new ObjectMapper().readTree(schemaStream);
      return Optional.of(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7).getSchema(schemaNode));

    } catch (Exception e) {
      log.error("Failed to load JSON schema from file: {}", schemaPath, e);
    }

    return Optional.empty();
  }

}
