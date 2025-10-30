package com.funding.velocity.service;

import com.funding.velocity.entity.InboundLog;
import com.funding.velocity.entity.OutboundLog;
import com.funding.velocity.repository.InboundLogRepository;
import com.funding.velocity.repository.OutboundLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoggingService {

  // TODO set the traceId here

  private final InboundLogRepository inboundLogRepository;
  private final OutboundLogRepository outboundLogRepository;


  public LoggingService(InboundLogRepository inboundLogRepository,
                        OutboundLogRepository outboundLogRepository) {

    this.inboundLogRepository = inboundLogRepository;
    this.outboundLogRepository = outboundLogRepository;
  }

  public void writeInboundLog(InboundLog inboundLog) {

    inboundLog.setTraceId("");

    inboundLogRepository.save(inboundLog);
  }

  public void writeOutboundLog(OutboundLog outboundLog) {

    outboundLog.setTraceId("");

    outboundLogRepository.save(outboundLog);
  }
}
