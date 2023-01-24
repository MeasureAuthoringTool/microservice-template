package cms.gov.madie.measure.services;

import cms.gov.madie.measure.config.FhirServicesConfig;
import cms.gov.madie.measure.config.VirusScanConfig;
import gov.cms.madie.models.scanner.VirusScanResponseDto;
import gov.cms.madie.models.scanner.VirusScanResultDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VirusScanClientTest {

  @Mock
  private VirusScanConfig virusScanConfig;

  @Mock private RestTemplate virusScanRestTemplate;

  @InjectMocks
  private VirusScanClient virusScanClient;

  @Test
  void testScanFileHandlesPositive() {
    Resource resource = Mockito.mock(Resource.class);
    when(virusScanConfig.getBaseUrl()).thenReturn("base.url");
    when(virusScanConfig.getScanFileUri()).thenReturn("/scan-file");
    when(virusScanConfig.getApiKey()).thenReturn("FAKE");
    VirusScanResponseDto scanResponseDto = VirusScanResponseDto.builder()
        .filesScanned(1)
        .cleanFileCount(1)
        .scanResults(List.of(
            VirusScanResultDto.builder()
                .fileName("TestFile.txt")
                .infected(false)
                .viruses(null)
                .build()
        ))
        .build();
    ResponseEntity<VirusScanResponseDto> response = ResponseEntity.ok(scanResponseDto);
    when(virusScanRestTemplate.exchange(any(RequestEntity.class), any(Class.class)))
        .thenReturn(response);
    VirusScanResponseDto output = virusScanClient.scanFile(resource);
    assertThat(output, is(equalTo(scanResponseDto)));
  }
  @Test
  void testScanFileHandlesNegative() {
    Resource resource = Mockito.mock(Resource.class);
    when(virusScanConfig.getBaseUrl()).thenReturn("base.url");
    when(virusScanConfig.getScanFileUri()).thenReturn("/scan-file");
    when(virusScanConfig.getApiKey()).thenReturn("FAKE");

    VirusScanResponseDto scanResponseDto = VirusScanResponseDto.builder()
        .filesScanned(1)
        .cleanFileCount(0)
        .infectedFileCount(1)
        .scanResults(List.of(
            VirusScanResultDto.builder()
                .fileName("TestFile.txt")
                .infected(true)
                .viruses(List.of("SomeBadVirus", "LessBadVirusButStillBad"))
                .build()
        ))
        .build();
    ResponseEntity<VirusScanResponseDto> response = ResponseEntity.ok(scanResponseDto);
    when(virusScanRestTemplate.exchange(any(RequestEntity.class), any(Class.class)))
        .thenReturn(response);
    VirusScanResponseDto output = virusScanClient.scanFile(resource);
    assertThat(output, is(equalTo(scanResponseDto)));
  }
}
