package cms.gov.madie.measure.services;

import cms.gov.madie.measure.config.QdmServiceConfig;
import cms.gov.madie.measure.exceptions.InternalServerException;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.measure.Measure;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class QdmPackageServiceTest {
  @Mock private QdmServiceConfig qdmServiceConfig;
  @Mock private RestTemplate qdmServiceRestTemplate;
  @InjectMocks private QdmPackageService qdmPackageService;

  private final String token = "token";
  private Measure measure;

  @BeforeEach
  void setUp() {
    measure =
        Measure.builder()
            .id("1")
            .ecqmTitle("test")
            .cql("fake cql")
            .model(String.valueOf(ModelType.QDM_5_6))
            .build();
    when(qdmServiceConfig.getBaseUrl()).thenReturn("baseurl");
  }

  @Test
  void getCreateMeasurePackage() {
    when(qdmServiceConfig.getCreatePackageUrn()).thenReturn("/elm/uri");
    String packageContent = "Measure Package Contents";
    when(qdmServiceRestTemplate.exchange(
            any(URI.class), eq(HttpMethod.PUT), any(HttpEntity.class), any(Class.class)))
        .thenReturn(ResponseEntity.ok(packageContent.getBytes()));
    byte[] packageContents = qdmPackageService.getMeasurePackage(measure, token);
    assertThat(packageContents, is(notNullValue()));
    assertThat(new String(packageContents), is(equalTo(packageContent)));
  }

  @Test
  void getCreateMeasurePackageWhenQdmServiceReturnedErrors() {
    when(qdmServiceConfig.getCreatePackageUrn()).thenReturn("/elm/uri");
    when(qdmServiceRestTemplate.exchange(
            any(URI.class), eq(HttpMethod.PUT), any(HttpEntity.class), any(Class.class)))
        .thenThrow(new RestClientException("something went wrong"));
    String errorMessage = "An error occurred while creating a measure package.";
    Exception ex =
        assertThrows(
            InternalServerException.class,
            () -> qdmPackageService.getMeasurePackage(measure, token),
            errorMessage);
    assertThat(ex.getMessage(), is(equalTo(errorMessage)));
  }

  @Test
  void testGetQRDASuccess() {
    when(qdmServiceConfig.getCreateQrdaUrn()).thenReturn("/qrda");
    String qrdaContent = "Test QRDA";
    when(qdmServiceRestTemplate.exchange(
            any(URI.class), eq(HttpMethod.PUT), any(HttpEntity.class), any(Class.class)))
        .thenReturn(ResponseEntity.ok(qrdaContent.getBytes()));
    byte[] qrda = qdmPackageService.getQRDA(measure, token);
    assertThat(qrda, is(notNullValue()));
    assertThat(new String(qrda), is(equalTo(qrdaContent)));
  }

  @Test
  void testGetQRDAThrowsRestClientException() {
    when(qdmServiceConfig.getCreateQrdaUrn()).thenReturn("/qrda");
    when(qdmServiceRestTemplate.exchange(
            any(URI.class), eq(HttpMethod.PUT), any(HttpEntity.class), any(Class.class)))
        .thenThrow(new RestClientException("something went wrong"));
    String errorMessage =
        "An error occurred while creating QRDA for QDM measure: 1, please check qdm service logs for more information";
    Exception ex =
        assertThrows(
            InternalServerException.class,
            () -> qdmPackageService.getQRDA(measure, token),
            errorMessage);
    assertThat(ex.getMessage(), is(equalTo("An error occurred while creating a QRDA.")));
  }
}
