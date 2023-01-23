package cms.gov.madie.measure.services;

import cms.gov.madie.measure.config.VirusScanConfig;
import cms.gov.madie.measure.resources.ScanController;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
import org.springframework.validation.ObjectError;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class VirusScanClient {
  private VirusScanConfig virusScanConfig;
  private RestTemplate virusScanRestTemplate;

  public VirusScanResponseDto scanFile(Resource fileResource) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    headers.set("apikey", virusScanConfig.getApiKey());

    MultiValueMap<String, Object> body
        = new LinkedMultiValueMap<>();
    body.add("file", fileResource);
    final String virusScanUrl = virusScanConfig.getBaseUrl() + virusScanConfig.getScanFileUri();
    final URI uri = URI.create(virusScanUrl);
    ResponseEntity<VirusScanResponseDto> response = virusScanRestTemplate.exchange(
        new RequestEntity(body, headers, HttpMethod.POST, uri),
        VirusScanResponseDto.class
    );
    return response.getBody();
  }







  @Data
  @Builder(toBuilder = true)
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ScanValidationDto {
    private boolean valid;
    private ObjectError error;
  }

  @Data
  public static class VirusScanResponseDto {
    private int filesScanned;
    private int infectedFileCount;
    private int cleanFileCount;
    private List<VirusScanResultDto> scanResults;
  }

  @Data
  public static class VirusScanResultDto {
    private String fileName;
    private boolean infected;
    private List<String> viruses;
  }
}
