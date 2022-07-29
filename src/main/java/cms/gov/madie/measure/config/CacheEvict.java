package cms.gov.madie.measure.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Objects;

@Configuration
public class CacheEvict {

  @Autowired private CacheManager cacheManager;

  // clearing cache after 1 hour
  @Scheduled(fixedRate = 6000 * 60 * 60)
  public void evictAllCachesAtIntervals() {
    cacheManager
        .getCacheNames()
        .forEach(cacheName -> Objects.requireNonNull(cacheManager.getCache(cacheName)).clear());
  }
}
