package com.funding.velocity.service;

import static com.funding.velocity.constant.JsonFields.ACCEPTED;
import static com.funding.velocity.constant.JsonFields.CUSTOMER_ID;
import static com.funding.velocity.constant.JsonFields.ID;
import static com.funding.velocity.constant.JsonFields.LOAD_AMOUNT;
import static com.funding.velocity.constant.JsonFields.TIME;
import static com.funding.velocity.constant.MdcValues.TRACE_ID;

import com.funding.velocity.config.FundingLimitConfig;
import com.funding.velocity.entity.CustomerTransaction;
import com.funding.velocity.entity.OutboundLog;
import com.funding.velocity.repository.CustomerTransactionRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
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

  private static final String TIMEZONE = "UTC";

  private final CustomerTransactionRepository customerTransactionRepository;
  private final LoggingService loggingService;

  private final Integer dailyAmountLimit;
  private final Integer weeklyAmountLimit;
  private final Integer dailyTransactionLimit;

  private final Cache dailyCache;
  private final Cache weeklyCache;

  public LoadFundingService(CustomerTransactionRepository customerTransactionRepository,
                            LoggingService loggingService,
                            FundingLimitConfig fundingLimitConfig,
                            CacheManager cacheManager) {

    this.customerTransactionRepository = customerTransactionRepository;
    this.loggingService = loggingService;

    Map<String, Integer> amountsMap = fundingLimitConfig.getAmounts();
    Map<String, Integer> loadsMap = fundingLimitConfig.getLoads();

    dailyAmountLimit = amountsMap.get("daily");
    weeklyAmountLimit = amountsMap.get("weekly");
    dailyTransactionLimit = loadsMap.get("daily");

    dailyCache = cacheManager.getCache("dailyCache");
    weeklyCache = cacheManager.getCache("weeklyCache");
  }

  public JSONObject loadFunds(JSONObject json) {

    CustomerTransaction transaction;
    JSONObject response = new JSONObject();

    String customerId = json.getString(CUSTOMER_ID);
    String datetime = json.getString(TIME);
    Optional<Double> loadAmount = parseDollarAmount(json.getString(LOAD_AMOUNT));

    response.put(ID, json.get(ID));
    response.put(CUSTOMER_ID, customerId);
    response.put(ACCEPTED, false);

    if (loadAmount.map(amount -> amount <= dailyAmountLimit).orElse(false)
        && isFundRequestValid(customerId, datetime, loadAmount.get())) {

      log.info("Funded transaction for customer {} is valid", customerId);

      transaction = CustomerTransaction.builder()
          .traceId(MDC.get(TRACE_ID))
          .requestId(json.getString(ID))
          .customerId(customerId)
          .loadAmount(loadAmount.get())
          .time(LocalDateTime.parse(json.getString(TIME), DateTimeFormatter.ISO_DATE_TIME))
          .build();

      customerTransactionRepository.save(transaction);

      response.put(ACCEPTED, true);
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
    LocalDate localDate = zonedDateTime.toLocalDate();

    ZonedDateTime dayStart = zonedDateTime.toLocalDate().atStartOfDay(ZoneId.of(TIMEZONE));
    ZonedDateTime dayEnd = zonedDateTime.toLocalDate()
        .atTime(LocalTime.MAX)
        .atZone(ZoneId.of(TIMEZONE));
    ZonedDateTime endOfWeek = zonedDateTime.toLocalDate()
        .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        .atTime(LocalTime.MAX)
        .atZone(ZoneId.of(TIMEZONE));

    BigDecimal transactionCount = cacheResult(
        dailyCache,
        "transactionCount:" + customerId + localDate, // transaction key construction
        () -> BigDecimal.valueOf(
            customerTransactionRepository.findNumberOfTransactionsByCustomerIdAndTime(dayStart, dayEnd, customerId)
        ),
        BigDecimal.valueOf(dailyTransactionLimit)
    );

    BigDecimal dailySum = cacheResult(
        dailyCache,
        "dailySum:" + customerId + localDate, // daily key construction
        () -> Optional.ofNullable(
                customerTransactionRepository.findSumOfLoadedAmountBetweenDatesByCustomerId(dayStart, dayEnd, customerId))
            .map(BigDecimal::valueOf)
            .orElse(BigDecimal.ZERO),
        BigDecimal.valueOf(dailyAmountLimit)
    );

    BigDecimal weeklySum = cacheResult(
        weeklyCache,
        "weeklySum:" + customerId + localDate, // weekly key construction
        () -> Optional.ofNullable(
                customerTransactionRepository.findSumOfLoadedAmountBetweenDatesByCustomerId(dayStart, endOfWeek, customerId))
            .map(BigDecimal::valueOf)
            .orElse(BigDecimal.ZERO),
        BigDecimal.valueOf(weeklyAmountLimit)
    );

    BigDecimal dailySumWithAttempt = dailySum.add(BigDecimal.valueOf(loadAmount));
    BigDecimal weeklySumWithAttempt = weeklySum.add(BigDecimal.valueOf(loadAmount));

    return transactionCount.intValue() <= dailyTransactionLimit
        && dailySumWithAttempt.compareTo(BigDecimal.valueOf(dailyAmountLimit)) <= 0
        && weeklySumWithAttempt.compareTo(BigDecimal.valueOf(weeklyAmountLimit)) <= 0;
  }

  private BigDecimal cacheResult(Cache cache, String key, Supplier<BigDecimal> supplier, BigDecimal threshold) {

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

  private Optional<Double> parseDollarAmount(String dollarAmount) {

    try {
      return Optional.of(Double.parseDouble(dollarAmount.replace("$", "").replace(",", "")));

    } catch (NumberFormatException e) {
      log.warn("Unable to parse loaned amount: {}", dollarAmount);
      return Optional.empty();
    }
  }
}
