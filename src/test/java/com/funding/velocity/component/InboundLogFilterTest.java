package com.funding.velocity.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.funding.velocity.BaseTest;
import com.funding.velocity.entity.InboundLog;
import com.funding.velocity.repository.InboundLogRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class InboundLogFilterTest extends BaseTest {

  @Mock
  private InboundLogRepository inboundLogRepository;

  @Mock
  private FilterChain filterChain;

  @InjectMocks
  private InboundLogFilter inboundLogFilter;

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() {

    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
  }

  @Test
  void request_doFilter_saveInboundLog() throws IOException, ServletException {

    request.setRequestURI("/api/test");
    request.setMethod("POST");
    request.setContent("{\"customer_id\":\"1\"}".getBytes(StandardCharsets.UTF_8));

    inboundLogFilter.doFilter(request, response, filterChain);

    ArgumentCaptor<InboundLog> captor = ArgumentCaptor.forClass(InboundLog.class);
    verify(inboundLogRepository, times(1)).save(captor.capture());

    InboundLog savedLog = captor.getValue();

    assertEquals("/api/test", savedLog.getPath());
    assertEquals("POST", savedLog.getMethod());
    assertNotNull(savedLog.getTraceId());
    assertFalse(savedLog.getTraceId().isBlank());
  }

}
