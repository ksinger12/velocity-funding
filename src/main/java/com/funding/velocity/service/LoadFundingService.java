package com.funding.velocity.service;

import com.funding.velocity.config.FundingLimitConfig;
import com.funding.velocity.entity.CustomerTransaction;
import com.funding.velocity.entity.OutboundLog;
import com.funding.velocity.repository.CustomerTransactionRepository;
import com.networknt.schema.JsonSchema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import org.springframework.cache.Cache;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoadFundingService {

  private final CustomerTransactionRepository customerTransactionRepository;
  private final LoggingService loggingService;

  private final Map<String, Integer> amountsMap;
  private final Map<String, Integer> loadsMap;

  private final Cache dailyCache;
  private final Cache weeklyCache;

  public LoadFundingService(CustomerTransactionRepository customerTransactionRepository,
      LoggingService loggingService,
      FundingLimitConfig fundingLimitConfig,
      CacheManager cacheManager) {

    this.customerTransactionRepository = customerTransactionRepository;
    this.loggingService = loggingService;

    amountsMap = fundingLimitConfig.getAmounts();
    loadsMap = fundingLimitConfig.getLoads();

    this.dailyCache = cacheManager.getCache("dailyCache");
    this.weeklyCache = cacheManager.getCache("weeklyCache");
  }

  public JSONObject loadFunds(JSONObject json) {

    String customerId = json.getString("customer_id");
    CustomerTransaction transaction = null;
    JSONObject response = new JSONObject();

    response.put("id", json.get("id"));
    response.put("customer_id", customerId);
    response.put("accepted", false);

    if (isFundRequestValid(customerId)) {
      log.info("Funded transaction for customer {} is valid", customerId);

      transaction = CustomerTransaction.builder()
          .requestId(json.getString("id"))
          .customerId(customerId)
          .loadAmount(parseDollarAmount(json.getString("load_amount")).orElse(null))
          .time(LocalDateTime.parse(json.getString("time"), DateTimeFormatter.ISO_DATE_TIME))
          .build();

      customerTransactionRepository.save(transaction);

      response.put("accepted", true);
    }

    OutboundLog outbound = OutboundLog.builder()
        .customerId(customerId)
        .payload(response.toString())
        .build();

    loggingService.writeOutboundLog(outbound);

    return response;
  }

  private boolean isFundRequestValid(String customerId) {

    ZonedDateTime todayStart = LocalDate.now(ZoneOffset.UTC)
        .atStartOfDay(ZoneId.of("UTC"));

    ZonedDateTime todayEnd = LocalDate.now(ZoneOffset.UTC)
        .atTime(LocalTime.MAX)
        .atZone(ZoneId.of("UTC"));

    ZonedDateTime endOfWeek = LocalDate.now(ZoneOffset.UTC)
        .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        .atTime(LocalTime.MAX)
        .atZone(ZoneId.of("UTC"));

    BigDecimal transactionCount = cacheResult(
        dailyCache,
        "transactionCount:" + customerId,
        () -> BigDecimal.valueOf(customerTransactionRepository.findNumberOfTransactionsByCustomerIdAndTime(todayStart, todayEnd, customerId)),
        BigDecimal.valueOf(loadsMap.get("daily"))
    );

    BigDecimal dailySum = cacheResult(
        dailyCache,
        "dailySum:" + customerId,
        () -> Optional.ofNullable(customerTransactionRepository.findSumOfLoadedAmountBetweenDatesByCustomerId(todayStart, todayEnd, customerId))
                      .map(BigDecimal::valueOf)
                      .orElse(BigDecimal.ZERO),
        BigDecimal.valueOf(amountsMap.get("daily"))
    );

    BigDecimal weeklySum = cacheResult(
        weeklyCache,
        "weeklySum:" + customerId,
        () -> Optional.ofNullable(customerTransactionRepository.findSumOfLoadedAmountBetweenDatesByCustomerId(todayStart, endOfWeek, customerId))
                      .map(BigDecimal::valueOf)
                      .orElse(BigDecimal.ZERO),
        BigDecimal.valueOf(amountsMap.get("weekly"))
    );

    return transactionCount.intValue() <= loadsMap.get("daily")
        && dailySum.doubleValue() <= amountsMap.get("daily")
        && weeklySum.doubleValue() <= amountsMap.get("weekly");
  }

  private BigDecimal cacheResult(Cache cache, String key, Supplier<BigDecimal> supplier, BigDecimal threshold) {

    BigDecimal cached = cache.get(key, BigDecimal.class);
    if (cached != null) {
      return cached;
    }

    BigDecimal result = supplier.get();

    if (result == null) {
      result = BigDecimal.ZERO;
    }

    // cache the result if the threshold is met
    if (result.compareTo(threshold) >= 0) {
      cache.put(key, result);
    }

    return result;
  }


  private Optional<Double> parseDollarAmount(String dollarAmount) {

    try {
      return Optional.of(Double.parseDouble(dollarAmount.replace("$", "").replace(",", "")));

    } catch (NumberFormatException e) {
      log.warn("Unable to parse loaned amount: {}", dollarAmount);
      return Optional.empty();
    }
  }
}
