package com.funding.velocity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.funding.velocity.BaseTest;
import com.funding.velocity.config.SchemaConfig;
import com.funding.velocity.service.LoadFundingService;
import java.util.Map;
import java.util.stream.Stream;
import org.json.JSONObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class LoadFundingControllerTest extends BaseTest {

  @Mock
  private LoadFundingService loadFundingService;

  @Mock
  private SchemaConfig schemaConfig;

  @InjectMocks
  private LoadFundingController controller;

  @ParameterizedTest
  @MethodSource("loadAmountExamples")
  void payload_loadFunds_responseReceived(String payload, boolean accepted, int expectedServiceCallCount) throws JsonProcessingException {

    JSONObject mockResponse = new JSONObject();
    mockResponse.put("accepted", accepted);

    when(loadFundingService.loadFunds(any(JSONObject.class))).thenReturn(mockResponse);
    when(schemaConfig.getSchemas()).thenReturn(Map.of("LoadFunds", "schemas/loadFundsSchema.json"));

    ResponseEntity<String> response = controller.loadFunds(payload);

    if (accepted) {
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    } else {
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    verify(loadFundingService, times(expectedServiceCallCount)).loadFunds(any(JSONObject.class));
  }

  private static Stream<Arguments> loadAmountExamples() {

    return Stream.of(
        Arguments.of("{\"id\":\"1\",\"customer_id\":\"100\",\"load_amount\":\"$100.00\",\"time\":\"2000-01-01T00:00:00Z\"}", true, 1),
        Arguments.of("{\"id\":\"2\",\"customer_id\":\"101\",\"load_amount\":\"\",\"time\":\"2000-01-01T01:00:00Z\"}", false, 1),
        Arguments.of("{\"id\":\"3\",\"customer_id\":\"102\"}", false, 0)
    );
  }
}
