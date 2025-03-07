package cms.gov.madie.measure.services;

import cms.gov.madie.measure.config.VirusScanConfig;
import gov.cms.madie.models.scanner.VirusScanResponseDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Slf4j
@Service
@AllArgsConstructor
public class VirusScanClient {
  private VirusScanConfig virusScanConfig;
  private RestTemplate virusScanRestTemplate;

  public VirusScanResponseDto scanFile(Resource fileResource) {
    // return clean scan results if virus scanning is disabled
    if (virusScanConfig.isScanDisabled()) {
      log.info("Virus scanning is disabled.");
      return VirusScanResponseDto.builder().filesScanned(1).cleanFileCount(1).build();
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    headers.set("apikey", virusScanConfig.getApiKey());

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", fileResource);
    final String virusScanUrl = virusScanConfig.getBaseUrl() + virusScanConfig.getScanFileUri();
    final URI uri = URI.create(virusScanUrl);
    ResponseEntity<VirusScanResponseDto> response =
        virusScanRestTemplate.exchange(
            new RequestEntity(body, headers, HttpMethod.POST, uri), VirusScanResponseDto.class);
    return response.getBody();
  }
}
