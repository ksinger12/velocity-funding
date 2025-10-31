package com.funding.velocity.component;

import com.funding.velocity.entity.InboundLog;
import com.funding.velocity.repository.InboundLogRepository;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

@Component
public class InboundLogFilter implements Filter {

  private final InboundLogRepository inboundLogRepository;

  public InboundLogFilter(InboundLogRepository inboundLogRepository) {
    this.inboundLogRepository = inboundLogRepository;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    String traceId = UUID.randomUUID().toString();
    MDC.put("traceId", traceId);

    ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper((HttpServletRequest) request);
    chain.doFilter(wrappedRequest, response);

    String body = new String(wrappedRequest.getContentAsByteArray(), StandardCharsets.UTF_8);

    InboundLog log = InboundLog.builder()
        .traceId(traceId)
        .path(wrappedRequest.getRequestURI())
        .method(wrappedRequest.getMethod())
        .payload(body)
        .build();

    inboundLogRepository.save(log);
  }
}
