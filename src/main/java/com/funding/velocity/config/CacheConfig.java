package com.funding.velocity.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableCaching
@Configuration
public class CacheConfig {

  @Bean
  public Caffeine<Object, Object> caffeineConfig() {
    return Caffeine.newBuilder();
  }

  @Bean
  public CacheManager cacheManager() {

    var cacheManager = new SimpleCacheManager();

    var dailyCache = new CaffeineCache("dailyCache",
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofDays(1))
            .build());

    var weeklyCache = new CaffeineCache("weeklyCache",
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofDays(7))
            .build());

    cacheManager.setCaches(List.of(dailyCache, weeklyCache));

    return cacheManager;
  }


}
