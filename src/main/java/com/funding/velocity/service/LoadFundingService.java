package com.funding.velocity.service;

import com.funding.velocity.config.FundingLimitConfig;
import com.funding.velocity.entity.CustomerTransaction;
import com.funding.velocity.entity.OutboundLog;
import com.funding.velocity.repository.CustomerTransactionRepository;
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
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoadFundingService {

  private final CustomerTransactionRepository customerTransactionRepository;
  private final LoggingService loggingService;

  private final Map<String, Integer> amountsMap;
  private final Map<String, Integer> loadsMap;

  public LoadFundingService(CustomerTransactionRepository customerTransactionRepository,
                            LoggingService loggingService,
                            FundingLimitConfig fundingLimitConfig) {

    this.customerTransactionRepository = customerTransactionRepository;
    this.loggingService = loggingService;

    amountsMap = fundingLimitConfig.getAmounts();
    loadsMap = fundingLimitConfig.getLoads();
  }

  public Optional<CustomerTransaction> loadFunds(JSONObject json) {

    String customerId = json.getString("customer_id");
    CustomerTransaction transaction = null;

    OutboundLog outbound = OutboundLog.builder()
        .customerId(customerId)
        .wasSuccessful(false)
        .build();

    if (isFundRequestValid(json)) {
      log.info("Funded transaction for customer {} is valid", customerId);

      transaction = CustomerTransaction.builder()
          .requestId(json.getString("id"))
          .customerId(customerId)
          .loadAmount(parseDollarAmount(json.getString("loan_amount")).orElse(null))
          .time(ZonedDateTime.parse(json.getString("time"), DateTimeFormatter.ISO_DATE_TIME))
          .build();

      customerTransactionRepository.save(transaction);
      outbound.setWasSuccessful(true);
    }

    loggingService.writeOutboundLog(outbound);

    return Optional.ofNullable(transaction);
  }

  private boolean isFundRequestValid(JSONObject json) {

    String customerId = json.getString("customerId");

    ZonedDateTime today = LocalDate.now(ZoneOffset.UTC)
        .atStartOfDay(ZoneId.of("UTC"));

    ZonedDateTime endOfToday = LocalDate.now(ZoneOffset.UTC)
        .atTime(LocalTime.MAX)
        .atZone(ZoneId.of("UTC"));

    ZonedDateTime endOfWeek = LocalDate.now(ZoneOffset.UTC)
        .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        .atTime(LocalTime.MAX)
        .atZone(ZoneId.of("UTC"));

    return customerTransactionRepository.findNumberOfTransactionsByCustomerIdAndTime(today, endOfToday, customerId) <= loadsMap.get("daily")
        && customerTransactionRepository.findSumOfLoadedAmountBetweenDatesByCustomerId(today, endOfToday, customerId) <= amountsMap.get("daily")
        && customerTransactionRepository.findSumOfLoadedAmountBetweenDatesByCustomerId(today, endOfWeek, customerId) <= amountsMap.get("weekly");
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
