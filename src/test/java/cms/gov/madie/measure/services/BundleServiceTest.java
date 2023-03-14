package cms.gov.madie.measure.services;

import cms.gov.madie.measure.exceptions.BundleOperationException;
import cms.gov.madie.measure.exceptions.CqlElmTranslationErrorException;
import cms.gov.madie.measure.exceptions.CqlElmTranslationServiceException;
import cms.gov.madie.measure.exceptions.InvalidResourceBundleStateException;
import cms.gov.madie.measure.utils.ResourceUtil;
import gov.cms.madie.models.measure.ElmJson;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.PopulationType;
import gov.cms.madie.models.common.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BundleServiceTest implements ResourceUtil {

  @Mock private FhirServicesClient fhirServicesClient;

  @Mock private ElmTranslatorClient elmTranslatorClient;

  @InjectMocks private BundleService bundleService;

  private Measure measure;

  @BeforeEach
  public void setUp() {
    Group group =
        Group.builder()
            .id("xyz-p12r-12ert")
            .populationBasis("Encounter")
            .populations(
                List.of(
                    new Population(
                        "id-1", PopulationType.INITIAL_POPULATION, "FactorialOfFive", null, null)))
            .groupDescription("Description")
            .scoringUnit("test-scoring-unit")
            .build();

    List<Group> groups = new ArrayList<>();
    groups.add(group);
    String elmJson = getData("/test_elm.json");
    measure =
        Measure.builder()
            .active(true)
            .id("xyz-p13r-13ert")
            .cql("test cql")
            .cqlErrors(false)
            .elmJson(elmJson)
            .measureSetId("IDIDID")
            .measureName("MSR01")
            .version(new Version(0, 0, 1))
            .groups(groups)
            .createdAt(Instant.now())
            .createdBy("test user")
            .lastModifiedAt(Instant.now())
            .lastModifiedBy("test user")
            .build();
  }

  @Test
  void testBundleMeasureReturnsNullForNullMeasure() {
    String output = bundleService.bundleMeasure(null, "Bearer TOKEN", "calculation");
    assertThat(output, is(nullValue()));
  }

  @Test
  void testBundleMeasureWhenThereIsNoCql() {
    measure.setCql(null);
    assertThrows(
        InvalidResourceBundleStateException.class,
        () -> bundleService.bundleMeasure(measure, "Bearer TOKEN", "calculation"));
  }

  @Test
  void testBundleMeasureWhenThereAreCqlErrors() {
    measure.setCqlErrors(true);
    assertThrows(
        InvalidResourceBundleStateException.class,
        () -> bundleService.bundleMeasure(measure, "Bearer TOKEN", "calculation"));
  }

  @Test
  void testBundleMeasureWhenThereAreNoGroups() {
    measure.setGroups(new ArrayList<>());
    assertThrows(
        InvalidResourceBundleStateException.class,
        () -> bundleService.bundleMeasure(measure, "Bearer TOKEN", "calculation"));
  }

  @Test
  void testBundleMeasureThrowsOperationException() {
    when(elmTranslatorClient.getElmJson(anyString(), anyString()))
        .thenReturn(ElmJson.builder().json("{}").xml("<></>").build());
    when(fhirServicesClient.getMeasureBundle(any(Measure.class), anyString(), anyString()))
        .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));
    assertThrows(
        BundleOperationException.class,
        () -> bundleService.bundleMeasure(measure, "Bearer TOKEN", "calculation"));
  }

  @Test
  void testBundleMeasureThrowsCqlElmTranslationServiceException() {
    when(elmTranslatorClient.getElmJson(anyString(), anyString()))
        .thenThrow(
            new CqlElmTranslationServiceException(
                "There was an error calling CQL-ELM translation service", new Exception()));
    assertThrows(
        CqlElmTranslationServiceException.class,
        () -> bundleService.bundleMeasure(measure, "Bearer TOKEN", "calculation"));
  }

  @Test
  void testBundleMeasureThrowsCqlElmTranslatorExceptionWithErrors() {
    when(elmTranslatorClient.getElmJson(anyString(), anyString()))
        .thenReturn(ElmJson.builder().json("{}").xml("<></>").build());
    when(elmTranslatorClient.hasErrors(any(ElmJson.class))).thenReturn(true);
    assertThrows(
        CqlElmTranslationErrorException.class,
        () -> bundleService.bundleMeasure(measure, "Bearer TOKEN", "calculation"));
  }

  @Test
  void testBundleMeasureReturnsBundleString() {
    final String json = "{\"message\": \"GOOD JSON\"}";
    when(fhirServicesClient.getMeasureBundle(any(Measure.class), anyString(), anyString()))
        .thenReturn(json);
    when(elmTranslatorClient.getElmJson(anyString(), anyString()))
        .thenReturn(ElmJson.builder().json("{}").xml("<></>").build());
    String output = bundleService.bundleMeasure(measure, "Bearer TOKEN", "calculation");
    assertThat(output, is(equalTo(json)));
  }
}
