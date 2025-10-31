package com.funding.velocity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@SpringBootTest(webEnvironment = DEFINED_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LoadFundsIntegrationTest {

  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate restTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private List<String> requests;
  private List<String> expectedResponses;

  @BeforeAll
  void setup() throws Exception {

    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(getClass().getResourceAsStream("/Venn - Back-End - Input.txt")))) {
      requests = reader.lines().toList();
    }
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(getClass().getResourceAsStream("/Venn - Back-End - Output .txt")))) {
      expectedResponses = reader.lines().toList();
    }
    assertThat(requests).hasSameSizeAs(expectedResponses);
  }

  @Test
  void vennInputFile_loadFundData_verifyVennOutputFileMatching() throws Exception {

    int passCount = 0;
    int failCount = 0;

    for (int i = 0; i < requests.size(); i++) {

      String reqJson = requests.get(i).trim();
      String expectedJson = expectedResponses.get(i).trim();

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      HttpEntity<String> requestEntity = new HttpEntity<>(reqJson, headers);

      ResponseEntity<String> response = restTemplate.postForEntity(
          "http://localhost:" + port + "/load-fund-data", requestEntity, String.class);

      JsonNode expectedNode = objectMapper.readTree(expectedJson);
      JsonNode actualNode = objectMapper.readTree(response.getBody().trim());

      if (expectedNode.equals(actualNode)) {
        log.info("Line " + (i + 1) + ": was successful");
        passCount++;
      } else {
        log.error("Line " + (i + 1) + ": failed");
        log.error("Request : " + reqJson);
        log.error("Expected: " + expectedJson);
        log.error("Got     : " + response.getBody());
        failCount++;
      }
    }

    assertThat(failCount).isEqualTo(0);
  }
}
