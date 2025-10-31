package com.funding.velocity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class LoadFundsHttpRunner {

  /**
   * Instructions on running:
   * 1. Ensure docker container is running
   * 2. Ensure the application is running
   * 3. Ensure the cache is empty and the database tables (inbound/outbound/client_transaction are truncated)
   * 4. Run this file
   */
  public static void main(String[] args) throws Exception {

    String url = "http://localhost:8080/load-fund-data";

    List<String> requests;
    List<String> expectedResponses;
    ObjectMapper objectMapper = new ObjectMapper();

    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(LoadFundsHttpRunner.class.getResourceAsStream("/Venn - Back-End - Input.txt")))) {
      requests = reader.lines().toList();
    }

    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(LoadFundsHttpRunner.class.getResourceAsStream("/Venn - Back-End - Output .txt")))) {
      expectedResponses = reader.lines().toList();
    }

    if (requests.size() != expectedResponses.size()) {
      throw new IllegalStateException("Request and expected response line counts do not match!");
    }

    HttpClient client = HttpClient.newHttpClient();
    int passCount = 0;
    int failCount = 0;

    System.out.println("Starting Load Funds HTTP Test...");

    for (int i = 0; i < requests.size(); i++) {
      String reqJson = requests.get(i).trim();
      String expectedJson = expectedResponses.get(i).trim();

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(reqJson))
          .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      String responseBody = response.body().trim();

      // Parse JSON and compare as JsonNode
      JsonNode expectedNode = objectMapper.readTree(expectedJson);
      JsonNode actualNode = objectMapper.readTree(responseBody);

      if (expectedNode.equals(actualNode)) {
        System.out.println("Line " + (i + 1) + ": PASS");
        passCount++;
      } else {
        System.out.println("Line " + (i + 1) + ": FAIL");
        System.out.println("Request : " + reqJson);
        System.out.println("Expected: " + expectedJson);
        System.out.println("Got     : " + responseBody);
        failCount++;
      }
    }

    System.out.println("Load Funds HTTP Test complete.");
    System.out.println("Passed: " + passCount + ", Failed: " + failCount);
  }

}