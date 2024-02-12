package cms.gov.madie.measure.services;

import cms.gov.madie.measure.config.AppConfigServiceConfig;
import cms.gov.madie.measure.dto.MadieFeatureFlag;
import cms.gov.madie.measure.dto.ServiceConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
public class AppConfigService {
  private final AppConfigServiceConfig appConfigServiceConfig;
  private final RestTemplate appConfigRestTemplate;
  private Map<String, Boolean> featureFlags;

  @Autowired
  public AppConfigService(
      RestTemplate appConfigRestTemplate, AppConfigServiceConfig appConfigServiceConfig) {
    this.appConfigRestTemplate = appConfigRestTemplate;
    this.appConfigServiceConfig = appConfigServiceConfig;
  }

  @PostConstruct
  @Scheduled(cron = "0 */5 * * * *")
  public void refreshAppConfig() {
    try {
      ServiceConfig serviceConfig =
          appConfigRestTemplate.getForObject(
              appConfigServiceConfig.getServiceConfigJsonUrl(), ServiceConfig.class);
      log.info("Initializing measure-service with serviceConfig: {}", serviceConfig);
      featureFlags = serviceConfig.getFeatures();
    } catch (Exception ex) {
      log.error("An error occurred while initializing feature flags from serviceConfig.json!", ex);
    }
  }

  public boolean isFlagEnabled(MadieFeatureFlag flag) {
    return featureFlags.getOrDefault(flag.toString(), false);
  }
}
