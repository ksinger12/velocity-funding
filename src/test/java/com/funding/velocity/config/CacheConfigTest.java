package com.funding.velocity.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.funding.velocity.BaseTest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;

class CacheConfigTest extends BaseTest {

  private CacheConfig cacheConfig;

  @BeforeEach
  void setUp() {

    cacheConfig = new CacheConfig();
  }

  @Test
  void initialization_caffeineConfig_configNotNull() {

    assertNotNull(cacheConfig.caffeineConfig());
  }

  @Test
  void initialized_cacheManager_dailyCacheAndWeeklyCache() {

    SimpleCacheManager cacheManager = (SimpleCacheManager) cacheConfig.cacheManager();
    cacheManager.afterPropertiesSet();

    Cache dailyCache = cacheManager.getCache("dailyCache");
    Cache weeklyCache = cacheManager.getCache("weeklyCache");

    assertNotNull(dailyCache);
    assertNotNull(weeklyCache);

    dailyCache.put("key1", "value1");
    weeklyCache.put("key2", "value2");

    assertEquals("value1", dailyCache.get("key1", String.class));
    assertEquals("value2", weeklyCache.get("key2", String.class));
  }

  @Test
  void initialized_cacheManager_dailyCacheExpiresAtEndOfDay() {

    SimpleCacheManager cacheManager = (SimpleCacheManager) cacheConfig.cacheManager();
    cacheManager.afterPropertiesSet();
    CaffeineCache dailyCache = (CaffeineCache) cacheManager.getCache("dailyCache");

    assertNotNull(dailyCache);
    var nativeCache = dailyCache.getNativeCache();
    var policy = nativeCache.policy().expireAfterWrite();
    assertTrue(policy.isPresent());

    Duration expectedDuration = Duration.between(
        LocalDateTime.now(ZoneId.of("UTC")),
        LocalDateTime.now(ZoneId.of("UTC")).withHour(23).withMinute(59).withSecond(59)
    );

    Duration actualDuration = policy.get().getExpiresAfter();
    assertTrue(Math.abs(actualDuration.minus(expectedDuration).toSeconds()) <= 1);
  }

  @Test
  void initialized_cacheManager_weeklyCacheExpiresAtEndOfWeek() {

    SimpleCacheManager cacheManager = (SimpleCacheManager) cacheConfig.cacheManager();
    cacheManager.afterPropertiesSet();
    CaffeineCache weeklyCache = (CaffeineCache) cacheManager.getCache("weeklyCache");

    assertNotNull(weeklyCache);
    var nativeCache = weeklyCache.getNativeCache();
    var policy = nativeCache.policy().expireAfterWrite();
    assertTrue(policy.isPresent());

    LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
    LocalDateTime endOfWeek = now.with(java.time.DayOfWeek.SUNDAY)
        .withHour(23).withMinute(59).withSecond(59);

    Duration expectedDuration = Duration.between(now, endOfWeek);

    Duration actualDuration = policy.get().getExpiresAfter();
    assertTrue(Math.abs(actualDuration.minus(expectedDuration).toSeconds()) <= 1);
  }
}
