package cms.gov.madie.measure.services;

import cms.gov.madie.measure.config.QdmServiceConfig;
import cms.gov.madie.measure.exceptions.InternalServerException;
import gov.cms.madie.models.measure.Measure;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Slf4j
@Service
@AllArgsConstructor
public class QdmPackageService implements PackageService {
  private final QdmServiceConfig qdmServiceConfig;
  private final RestTemplate qdmServiceRestTemplate;

  @Override
  public byte[] getMeasurePackage(Measure measure, String accessToken) {
    URI uri = URI.create(qdmServiceConfig.getBaseUrl() + qdmServiceConfig.getCreatePackageUrn());
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.AUTHORIZATION, accessToken);
    headers.set(HttpHeaders.ACCEPT, MediaType.ALL_VALUE);
    headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    HttpEntity<Measure> entity = new HttpEntity<>(measure, headers);
    try {
      log.info("requesting measure package for measure [{}] from qdm service", measure.getId());
      return qdmServiceRestTemplate.exchange(uri, HttpMethod.PUT, entity, byte[].class).getBody();
    } catch (RestClientException ex) {
      log.error(
          "An error occurred while creating package for QDM measure: "
              + measure.getId()
              + ", please check qdm service logs for more information",
          ex);
      throw new InternalServerException("An error occurred while creating a measure package.");
    }
  }
}
