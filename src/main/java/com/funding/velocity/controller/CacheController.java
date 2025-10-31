package com.funding.velocity.controller;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clear-cache")
public class CacheController {

  private final CacheManager cacheManager;

  public CacheController(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  @DeleteMapping("/daily")
  public ResponseEntity<String> clearDailyCache() {

    clearCache("dailyCache");
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/weekly")
  public ResponseEntity<String> clearWeeklyCache() {

    clearCache("weeklyCache");
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/all")
  public ResponseEntity<String> clearAllCaches() {

    cacheManager.getCacheNames().forEach(this::clearCache);
    return ResponseEntity.ok().build();
  }

  private void clearCache(String cacheName) {

    Cache cache = cacheManager.getCache(cacheName);

    if (cache != null) {
      cache.clear();
    }
  }

}
