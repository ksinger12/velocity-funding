package com.funding.velocity.util;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.cache.Cache;

public class CacheUtil {

  public static void incrementCache(Cache cache, String key, double increment) {

    BigDecimal current = cache.get(key, BigDecimal.class);
    if (current == null) current = BigDecimal.ZERO;

    cache.put(key, current.add(BigDecimal.valueOf(increment)));
  }

  public static BigDecimal getCachedValue(Cache cache, String key, Supplier<BigDecimal> supplier) {

    BigDecimal cached = cache.get(key, BigDecimal.class);
    if (cached != null) return cached;

    BigDecimal value = Optional.ofNullable(supplier.get()).orElse(BigDecimal.ZERO);
    cache.put(key, value);

    return value;
  }

}
