package com.funding.velocity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
import org.springframework.http.HttpStatus;
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
  }

  @Test
  void vennInputFile_loadFundData_verifyVennOutputFileMatching() throws Exception {

    int passCount = 0;
    int failCount = 0;

    int requestIndex = 0;
    int responseIndex = 0;

    File outputFile = new File("src/test/resources/generated_output.txt");
    try (BufferedWriter writer = new BufferedWriter(
        new FileWriter(outputFile, false))) { // overwrite each run

      for (requestIndex = 0; requestIndex < requests.size(); requestIndex++, responseIndex++) {

        String reqJson = requests.get(requestIndex).trim();
        String expectedJson = expectedResponses.get(responseIndex).trim();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(reqJson, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/load-fund-data", requestEntity, String.class);

        if (response.getStatusCode() == HttpStatus.NOT_MODIFIED) {
          responseIndex--;
          continue;
        }

        JsonNode expectedNode = objectMapper.readTree(expectedJson);
        JsonNode actualNode = objectMapper.readTree(response.getBody().trim());

        writer.write(objectMapper.writeValueAsString(actualNode));
        writer.newLine();

        if (expectedNode.equals(actualNode)) {
          log.info("Line " + (requestIndex + 1) + ": was successful");
          passCount++;
        } else {
          log.error("Line " + (requestIndex + 1) + ": failed");
          log.error("Request : " + reqJson);
          log.error("Expected: " + expectedJson);
          log.error("Got     : " + response.getBody());
          failCount++;
        }
      }
    }
    assertThat(failCount).isEqualTo(0);
  }
}
