package com.funding.velocity.service;

import static com.funding.velocity.constant.JsonFields.ACCEPTED;
import static com.funding.velocity.constant.JsonFields.CUSTOMER_ID;
import static com.funding.velocity.constant.JsonFields.ID;
import static com.funding.velocity.constant.JsonFields.LOAD_AMOUNT;
import static com.funding.velocity.constant.JsonFields.TIME;
import static com.funding.velocity.constant.MdcValues.TRACE_ID;

import com.funding.velocity.util.LoadFundingState;
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
import java.time.temporal.WeekFields;
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

    JSONObject response = new JSONObject();

    String customerId = json.getString(CUSTOMER_ID);
    String datetime = json.getString(TIME);
    Optional<Double> loadAmountOptional = parseDollarAmount(json.getString(LOAD_AMOUNT));

    response.put(ID, json.get(ID));
    response.put(CUSTOMER_ID, customerId);
    response.put(ACCEPTED, false);

    if (loadAmountOptional.isPresent()) {
      double loadAmount = loadAmountOptional.get();

      log.debug("Retrieved requested loadAmount from body {}", loadAmount);

      LoadFundingState loadFundingState = getFundState(customerId, datetime);

      // should customer be funded
      if (loadAmount <= dailyAmountLimit
          && loadFundingState.transactionCount() < dailyTransactionLimit
          && loadFundingState.dailySum() + loadAmount <= dailyAmountLimit
          && loadFundingState.weeklySum() + loadAmount <= weeklyAmountLimit) {

        log.info("Funding this customer: {} with the requested amount: {}", customerId, loadAmount);

        CustomerTransaction transaction = CustomerTransaction.builder()
            .traceId(MDC.get(TRACE_ID))
            .requestId(json.getString(ID))
            .customerId(customerId)
            .loadAmount(loadAmount)
            .time(LocalDateTime.parse(datetime, DateTimeFormatter.ISO_DATE_TIME))
            .build();

        customerTransactionRepository.save(transaction);

        updateCache(customerId, datetime, loadAmount);

        response.put(ACCEPTED, true);
      }
    }

    OutboundLog outbound = OutboundLog.builder()
        .customerId(customerId)
        .payload(response.toString())
        .build();

    loggingService.writeOutboundLog(outbound);

    return response;
  }

  private LoadFundingState getFundState(String customerId, String datetime) {

    ZonedDateTime zonedDateTime = ZonedDateTime.parse(datetime, DateTimeFormatter.ISO_DATE_TIME);
    LocalDate localDate = zonedDateTime.toLocalDate();

    WeekFields weekFields = WeekFields.of(DayOfWeek.MONDAY, 4);
    String localWeek = localDate.get(weekFields.weekBasedYear()) + "-" + localDate.get(weekFields.weekOfWeekBasedYear());

    String transactionCountKey = "transactionCount:" + customerId + localDate;
    String dailySumKey = "dailySum:" + customerId + localDate;
    String weeklySumKey = "weeklySum:" + customerId + localWeek;

    ZonedDateTime dayStart = localDate.atStartOfDay(ZoneId.of(TIMEZONE));
    ZonedDateTime dayEnd = localDate.atTime(LocalTime.MAX).atZone(ZoneId.of(TIMEZONE));
    ZonedDateTime weekStart = localDate.with(weekFields.dayOfWeek(), 1) // Monday
        .atStartOfDay(ZoneId.of(TIMEZONE));
    ZonedDateTime weekEnd = localDate.with(weekFields.dayOfWeek(), 7) // Sunday
        .atTime(LocalTime.MAX)
        .atZone(ZoneId.of(TIMEZONE));

    BigDecimal transactionCount = getCachedValue(dailyCache, transactionCountKey,
        () -> BigDecimal.valueOf(customerTransactionRepository
            .findNumberOfTransactionsByCustomerIdAndTime(dayStart, dayEnd, customerId)));

    BigDecimal dailySum = getCachedValue(dailyCache, dailySumKey,
        () -> Optional.ofNullable(customerTransactionRepository
                .findSumOfLoadedAmountBetweenDatesByCustomerId(dayStart, dayEnd, customerId))
            .map(BigDecimal::valueOf)
            .orElse(BigDecimal.ZERO));

    BigDecimal weeklySum = getCachedValue(weeklyCache, weeklySumKey,
        () -> Optional.ofNullable(customerTransactionRepository
                .findSumOfLoadedAmountBetweenDatesByCustomerId(weekStart, weekEnd, customerId))
            .map(BigDecimal::valueOf)
            .orElse(BigDecimal.ZERO));

    return new LoadFundingState(transactionCount.intValue(), dailySum.doubleValue(), weeklySum.doubleValue());
  }

  private void updateCache(String customerId, String datetime, double loadAmount) {

    ZonedDateTime zonedDateTime = ZonedDateTime.parse(datetime, DateTimeFormatter.ISO_DATE_TIME);
    LocalDate localDate = zonedDateTime.toLocalDate();

    WeekFields weekFields = WeekFields.of(DayOfWeek.MONDAY, 4);
    String localWeek = localDate.get(weekFields.weekBasedYear()) + "-" + localDate.get(weekFields.weekOfWeekBasedYear());

    String transactionCountKey = "transactionCount:" + customerId + localDate;
    String dailySumKey = "dailySum:" + customerId + localDate;
    String weeklySumKey = "weeklySum:" + customerId + localWeek;

    incrementCache(dailyCache, transactionCountKey, 1);
    incrementCache(dailyCache, dailySumKey, loadAmount);
    incrementCache(weeklyCache, weeklySumKey, loadAmount);
  }

  private BigDecimal getCachedValue(Cache cache, String key, Supplier<BigDecimal> supplier) {

    BigDecimal cached = cache.get(key, BigDecimal.class);
    if (cached != null) return cached;

    BigDecimal value = Optional.ofNullable(supplier.get()).orElse(BigDecimal.ZERO);
    cache.put(key, value);

    return value;
  }

  private void incrementCache(Cache cache, String key, double increment) {

    BigDecimal current = cache.get(key, BigDecimal.class);
    if (current == null) current = BigDecimal.ZERO;

    cache.put(key, current.add(BigDecimal.valueOf(increment)));
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
