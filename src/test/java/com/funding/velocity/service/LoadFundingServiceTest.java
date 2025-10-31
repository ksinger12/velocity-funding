package com.funding.velocity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.funding.velocity.BaseTest;
import com.funding.velocity.config.FundingLimitConfig;
import com.funding.velocity.entity.CustomerTransaction;
import com.funding.velocity.entity.OutboundLog;
import com.funding.velocity.repository.CustomerTransactionRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Stream;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

class LoadFundingServiceTest extends BaseTest {

  @Mock
  private CustomerTransactionRepository customerTransactionRepository;

  @Mock
  private LoggingService loggingService;

  @Mock
  private FundingLimitConfig fundingLimitConfig;

  @Mock
  private CacheManager cacheManager;

  @Mock
  private Cache dailyCache;

  @Mock
  private Cache weeklyCache;

  private LoadFundingService loadFundingService;

  @BeforeEach
  void setUp() {

    when(fundingLimitConfig.getAmounts()).thenReturn(Map.of("daily", 500, "weekly", 2000));
    when(fundingLimitConfig.getLoads()).thenReturn(Map.of("daily", 3));
    when(cacheManager.getCache("dailyCache")).thenReturn(dailyCache);
    when(cacheManager.getCache("weeklyCache")).thenReturn(weeklyCache);

    loadFundingService = new LoadFundingService(
        customerTransactionRepository,
        loggingService,
        fundingLimitConfig,
        cacheManager
    );
  }

  @ParameterizedTest
  @MethodSource("provideLoadFundsTestCases")
  void payload_loadFunds_verifyResponseAndRepoCalls(String payload, boolean accepted) {

    JSONObject json = new JSONObject(payload);

    when(customerTransactionRepository.findNumberOfTransactionsByCustomerIdAndTime(any(), any(), any()))
        .thenReturn(1);
    when(customerTransactionRepository.findSumOfLoadedAmountBetweenDatesByCustomerId(any(), any(), any()))
        .thenReturn(100.0);

    when(dailyCache.get(any(), eq(BigDecimal.class))).thenReturn(null);
    when(weeklyCache.get(any(), eq(BigDecimal.class))).thenReturn(null);

    JSONObject response = loadFundingService.loadFunds(json);

    assertEquals(accepted, response.getBoolean("accepted"));
    verify(loggingService, times(1)).writeOutboundLog(any(OutboundLog.class));

    if (accepted) {
      verify(customerTransactionRepository, times(1)).save(any(CustomerTransaction.class));
    } else {
      verify(customerTransactionRepository, never()).save(any(CustomerTransaction.class));
    }
  }

  private static Stream<Arguments> provideLoadFundsTestCases() {

    return Stream.of(
        Arguments.of("{\"id\":\"1\",\"customer_id\":\"100\",\"load_amount\":\"$100.00\",\"time\":\"2025-10-31T10:00:00Z\"}", true),
        Arguments.of("{\"id\":\"2\",\"customer_id\":\"101\",\"load_amount\":\"$600.00\",\"time\":\"2025-10-31T10:00:00Z\"}", false),
        Arguments.of("{\"id\":\"3\",\"customer_id\":\"103\",\"load_amount\":\"not-a-number\",\"time\":\"2025-10-31T10:00:00Z\"}", false)
    );
  }
}
