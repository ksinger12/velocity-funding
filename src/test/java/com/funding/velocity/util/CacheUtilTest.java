package com.funding.velocity.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

class CacheUtilTest {

  private Cache cache;

  @BeforeEach
  void setUp() {

    cache = mock(Cache.class);
  }

  @Test
  void emptyCache_incrementCache_putCalled() {

    when(cache.get("dailySum", BigDecimal.class)).thenReturn(null);

    CacheUtil.incrementCache(cache, "dailySum", 50.0);

    verify(cache).put("dailySum", BigDecimal.valueOf(50.0));
  }

  @Test
  void nonEmptyCache_incrementCache_putCalledWithSum() {

    when(cache.get("dailySum", BigDecimal.class)).thenReturn(BigDecimal.valueOf(100.0));

    CacheUtil.incrementCache(cache, "dailySum", 25.0);

    verify(cache).put("dailySum", BigDecimal.valueOf(125.0));
  }

  @Test
  void nonEmptyCache_getCachedValue_respondWithCachedValue() {

    Supplier<BigDecimal> supplier = mock(Supplier.class);

    when(cache.get("weeklySum", BigDecimal.class)).thenReturn(BigDecimal.valueOf(200.0));

    BigDecimal result = CacheUtil.getCachedValue(cache, "weeklySum", supplier);

    assertEquals(BigDecimal.valueOf(200.0), result);
    verifyNoInteractions(supplier);
  }

  @Test
  void emptyCache_getCachedValue_cacheUpdatedWithSupplier() {

    Supplier<BigDecimal> supplier = mock(Supplier.class);

    when(cache.get("weeklySum", BigDecimal.class)).thenReturn(null);
    when(supplier.get()).thenReturn(BigDecimal.valueOf(75.0));

    BigDecimal result = CacheUtil.getCachedValue(cache, "weeklySum", supplier);

    assertEquals(BigDecimal.valueOf(75.0), result);
    verify(supplier, times(1)).get();
    verify(cache).put("weeklySum", BigDecimal.valueOf(75.0));
  }

  @Test
  void emptyCacheAndEmptySupplierResult_getCachedValue_cachedUpdatedWithZero() {

    Supplier<BigDecimal> supplier = mock(Supplier.class);

    when(cache.get("weeklySum", BigDecimal.class)).thenReturn(null);
    when(supplier.get()).thenReturn(null);

    BigDecimal result = CacheUtil.getCachedValue(cache, "weeklySum", supplier);

    assertEquals(BigDecimal.ZERO, result);
    verify(cache).put("weeklySum", BigDecimal.ZERO);
  }
}

