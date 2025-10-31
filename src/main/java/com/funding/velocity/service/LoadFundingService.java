package com.funding.velocity.service;

import com.funding.velocity.config.FundingLimitConfig;
import com.funding.velocity.entity.CustomerTransaction;
import com.funding.velocity.entity.OutboundLog;
import com.funding.velocity.repository.CustomerTransactionRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.slf4j.MDC;
import org.springframework.cache.Cache;
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

    CustomerTransaction transaction;
    JSONObject response = new JSONObject();

    String customerId = json.getString("customer_id");
    String datetime = json.getString("time");
    Optional<Double> loadAmount = parseDollarAmount(json.getString("load_amount"));

    response.put("id", json.get("id"));
    response.put("customer_id", customerId);
    response.put("accepted", false);

    if (loadAmount.map(d -> d <= amountsMap.get("daily")).orElse(false)
        && isFundRequestValid(customerId, datetime, loadAmount.get())) {

      log.info("Funded transaction for customer {} is valid", customerId);

      transaction = CustomerTransaction.builder()
          .traceId(MDC.get("traceId"))
          .requestId(json.getString("id"))
          .customerId(customerId)
          .loadAmount(loadAmount.get())
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

  private boolean isFundRequestValid(String customerId, String datetime, Double loadAmount) {

    ZonedDateTime zonedDateTime = ZonedDateTime.parse(datetime, DateTimeFormatter.ISO_DATE_TIME);

    ZonedDateTime todayStart = zonedDateTime.toLocalDate().atStartOfDay(ZoneId.of("UTC"));
    ZonedDateTime todayEnd = zonedDateTime.toLocalDate()
        .atTime(LocalTime.MAX)
        .atZone(ZoneId.of("UTC"));
    ZonedDateTime endOfWeek = zonedDateTime.toLocalDate()
        .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        .atTime(LocalTime.MAX)
        .atZone(ZoneId.of("UTC"));

    BigDecimal transactionCount = cacheResult(
        dailyCache,
        "transactionCount:" + customerId + zonedDateTime.toLocalDate(),
        () -> BigDecimal.valueOf(
            customerTransactionRepository.findNumberOfTransactionsByCustomerIdAndTime(
                todayStart, todayEnd, customerId)),
        BigDecimal.valueOf(loadsMap.get("daily"))
    );

    BigDecimal dailySum = cacheResult(
        dailyCache,
        "dailySum:" + customerId + zonedDateTime.toLocalDate(),
        () -> Optional.ofNullable(
                customerTransactionRepository.findSumOfLoadedAmountBetweenDatesByCustomerId(
                    todayStart, todayEnd, customerId))
            .map(BigDecimal::valueOf)
            .orElse(BigDecimal.ZERO),
        BigDecimal.valueOf(amountsMap.get("daily"))
    );

    BigDecimal weeklySum = cacheResult(
        weeklyCache,
        "weeklySum:" + customerId + zonedDateTime.toLocalDate(),
        () -> Optional.ofNullable(
                customerTransactionRepository.findSumOfLoadedAmountBetweenDatesByCustomerId(
                    todayStart, endOfWeek, customerId))
            .map(BigDecimal::valueOf)
            .orElse(BigDecimal.ZERO),
        BigDecimal.valueOf(amountsMap.get("weekly"))
    );

    BigDecimal dailySumWithAttempt = dailySum.add(BigDecimal.valueOf(loadAmount));
    BigDecimal weeklySumWithAttempt = weeklySum.add(BigDecimal.valueOf(loadAmount));

    return transactionCount.intValue() <= loadsMap.get("daily")
        && dailySumWithAttempt.compareTo(BigDecimal.valueOf(amountsMap.get("daily"))) <= 0
        && weeklySumWithAttempt.compareTo(BigDecimal.valueOf(amountsMap.get("weekly"))) <= 0;
  }

  private BigDecimal cacheResult(Cache cache, String key, Supplier<BigDecimal> supplier,
      BigDecimal threshold) {

    BigDecimal cached = cache.get(key, BigDecimal.class);

    if (cached != null) {
      log.debug("Value is cached. Key: {}, Cached: {}", key, cached);
      return cached;
    }

    BigDecimal result = supplier.get();

    log.debug("Repository response for key: {}, result: {}", key, result);

    if (result == null) {
      result = BigDecimal.ZERO;
    }

    // cache the result only if the threshold is met
    if (result.compareTo(threshold) >= 0) {
      cache.put(key, result);
    }

    return result;
  }


//  private boolean isFundRequestValid(String customerId, String datetime, Double loadAmount) {
//
//    ZonedDateTime zonedDateTime = ZonedDateTime.parse(datetime, DateTimeFormatter.ISO_DATE_TIME);
//
//    ZonedDateTime todayStart = zonedDateTime.toLocalDate().atStartOfDay(ZoneId.of("UTC"));
//    ZonedDateTime todayEnd = zonedDateTime.toLocalDate().atTime(LocalTime.MAX)
//        .atZone(ZoneId.of("UTC"));
//    ZonedDateTime endOfWeek = zonedDateTime.toLocalDate()
//        .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
//        .atTime(LocalTime.MAX)
//        .atZone(ZoneId.of("UTC"));
//
//    BigDecimal transactionCount = cacheResult(
//        dailyCache,
//        "transactionCount:" + customerId + zonedDateTime.toLocalDate(),
//        () -> BigDecimal.valueOf(
//            customerTransactionRepository.findNumberOfTransactionsByCustomerIdAndTime(todayStart,
//                todayEnd, customerId)),
//        BigDecimal.valueOf(loadsMap.get("daily"))
//    );
//
//    BigDecimal dailySum = cacheResult(
//        dailyCache,
//        "dailySum:" + customerId + zonedDateTime.toLocalDate(),
//        () -> Optional.ofNullable(
//                customerTransactionRepository.findSumOfLoadedAmountBetweenDatesByCustomerId(todayStart,
//                    todayEnd, customerId))
//            .map(v -> BigDecimal.valueOf(v + loadAmount))
//            .orElse(BigDecimal.ZERO),
//        BigDecimal.valueOf(amountsMap.get("daily"))
//    );
//
//    BigDecimal weeklySum = cacheResult(
//        weeklyCache,
//        "weeklySum:" + customerId + zonedDateTime.toLocalDate(),
//        () -> Optional.ofNullable(
//                customerTransactionRepository.findSumOfLoadedAmountBetweenDatesByCustomerId(todayStart,
//                    endOfWeek, customerId))
//            .map(v -> BigDecimal.valueOf(v + loadAmount))
//            .orElse(BigDecimal.ZERO),
//        BigDecimal.valueOf(amountsMap.get("weekly"))
//    );
//
//    return transactionCount.intValue() <= loadsMap.get("daily")
//        && dailySum.doubleValue() <= amountsMap.get("daily")
//        && weeklySum.doubleValue() <= amountsMap.get("weekly");
//  }
//
//  private BigDecimal cacheResult(Cache cache, String key, Supplier<BigDecimal> supplier,
//      BigDecimal threshold) {
//
//    BigDecimal cached = cache.get(key, BigDecimal.class);
//
//    if (cached != null) {
//      log.debug("Value is cached. Key: {}, Cached: {}", key, cached);
//      return cached;
//    }
//
//    BigDecimal result = supplier.get();
//
//    log.debug("Repository count response for key: {}, result: {}", key, result);
//
//    if (result == null) {
//      result = BigDecimal.ZERO;
//    }
//
//    // cache the result if the threshold is met
//    if (result.compareTo(threshold) >= 0) {
//      cache.put(key, result);
//    }
//
//    return result;
//  }


  private Optional<Double> parseDollarAmount(String dollarAmount) {

    try {
      return Optional.of(Double.parseDouble(dollarAmount.replace("$", "").replace(",", "")));

    } catch (NumberFormatException e) {
      log.warn("Unable to parse loaned amount: {}", dollarAmount);
      return Optional.empty();
    }
  }
}
