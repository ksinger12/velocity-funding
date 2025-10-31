package com.funding.velocity.config;

import static java.time.DayOfWeek.SUNDAY;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.time.LocalDateTime;
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

    SimpleCacheManager cacheManager = new SimpleCacheManager();

    CaffeineCache dailyCache = new CaffeineCache("dailyCache",
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.between(
                LocalDateTime.now(),
                LocalDateTime.now().withHour(23).withMinute(59).withSecond(59)
            ))
            .build()
    );

    CaffeineCache weeklyCache = new CaffeineCache("weeklyCache",
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.between(
                LocalDateTime.now(),
                LocalDateTime.now()
                    .with(SUNDAY)
                    .withHour(23).withMinute(59).withSecond(59)
            ))
            .build()
    );

    cacheManager.setCaches(List.of(dailyCache, weeklyCache));

    return cacheManager;
  }

}
