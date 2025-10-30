package com.funding.velocity.service;

import com.funding.velocity.repository.CustomerTransactionRepository;
import com.funding.velocity.repository.InboundLogRepository;
import com.funding.velocity.repository.OutboundLogRepository;
import java.util.Optional;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class LoadFundingService {

  private final CustomerTransactionRepository customerTransactionRepository;
  private final OutboundLogRepository outboundLogRepository;

  public LoadFundingService(CustomerTransactionRepository customerTransactionRepository,
                            OutboundLogRepository outboundLogRepository) {

    this.customerTransactionRepository = customerTransactionRepository;
    this.outboundLogRepository = outboundLogRepository;
  }

  public Optional<String> loadFunds(JSONObject json) {

  }

  private boolean isFundRequestValid(JSONObject json) {


  }
}
