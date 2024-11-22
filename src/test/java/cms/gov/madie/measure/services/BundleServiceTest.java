package cms.gov.madie.measure.services;

import cms.gov.madie.measure.dto.PackageDto;
import cms.gov.madie.measure.exceptions.BundleOperationException;
import cms.gov.madie.measure.exceptions.CqlElmTranslationErrorException;
import cms.gov.madie.measure.exceptions.CqlElmTranslationServiceException;
import cms.gov.madie.measure.exceptions.InvalidResourceStateException;
import cms.gov.madie.measure.repositories.ExportRepository;
import cms.gov.madie.measure.utils.ResourceUtil;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.measure.ElmJson;
import gov.cms.madie.models.measure.Export;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureGroupTypes;
import gov.cms.madie.models.measure.MeasureMetaData;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.PopulationType;
import gov.cms.madie.models.common.Organization;
import gov.cms.madie.models.common.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@ExtendWith(MockitoExtension.class)
class BundleServiceTest implements ResourceUtil {

  @Mock private FhirServicesClient fhirServicesClient;
  @Mock private ElmTranslatorClient elmTranslatorClient;
  @Mock private ExportRepository exportRepository;

  @InjectMocks private BundleService bundleService;

  private Measure measure;

  @BeforeEach
  public void setUp() {
    Group group =
        Group.builder()
            .id("xyz-p12r-12ert")
            .populationBasis("Encounter")
            .measureGroupTypes(List.of(MeasureGroupTypes.PROCESS))
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
    MeasureMetaData metaData = MeasureMetaData.builder().draft(true).build();
    measure =
        Measure.builder()
            .active(true)
            .id("xyz-p13r-13ert")
            .cql("test cql")
            .model(ModelType.QDM_5_6.getValue())
            .cqlErrors(false)
            .elmJson(elmJson)
            .measureSetId("IDIDID")
            .measureName("MSR01")
            .version(new Version(0, 0, 1))
            .groups(groups)
            .measureMetaData(metaData)
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
        InvalidResourceStateException.class,
        () -> bundleService.bundleMeasure(measure, "Bearer TOKEN", "calculation"));
  }

  @Test
  void testBundleMeasureWhenThereAreCqlErrors() {
    measure.setCqlErrors(true);
    assertThrows(
        InvalidResourceStateException.class,
        () -> bundleService.bundleMeasure(measure, "Bearer TOKEN", "calculation"));
  }

  @Test
  void testBundleMeasureThrowsOperationException() {
    when(elmTranslatorClient.getElmJson(anyString(), anyString(), anyString()))
        .thenReturn(ElmJson.builder().json("{}").xml("<></>").build());
    when(fhirServicesClient.getMeasureBundle(any(Measure.class), anyString(), anyString()))
        .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));
    assertThrows(
        BundleOperationException.class,
        () -> bundleService.bundleMeasure(measure, "Bearer TOKEN", "calculation"));
  }

  @Test
  void testBundleMeasureThrowsCqlElmTranslationServiceException() {
    when(elmTranslatorClient.getElmJson(anyString(), anyString(), anyString()))
        .thenThrow(
            new CqlElmTranslationServiceException(
                "There was an error calling CQL-ELM translation service", new Exception()));
    assertThrows(
        CqlElmTranslationServiceException.class,
        () -> bundleService.bundleMeasure(measure, "Bearer TOKEN", "calculation"));
  }

  @Test
  void testBundleMeasureThrowsCqlElmTranslatorExceptionWithErrors() {
    when(elmTranslatorClient.getElmJson(anyString(), anyString(), anyString()))
        .thenReturn(ElmJson.builder().json("{}").xml("<></>").build());
    when(elmTranslatorClient.hasErrors(any(ElmJson.class))).thenReturn(true);
    assertThrows(
        CqlElmTranslationErrorException.class,
        () -> bundleService.bundleMeasure(measure, "Bearer TOKEN", "calculation"));
  }

  @Test
  void testBundleMeasureReturnsBundleStringForDraftMeasure() {
    final String json = "{\"message\": \"GOOD JSON\"}";
    when(fhirServicesClient.getMeasureBundle(any(Measure.class), anyString(), anyString()))
        .thenReturn(json);
    when(elmTranslatorClient.getElmJson(anyString(), anyString(), anyString()))
        .thenReturn(ElmJson.builder().json("{}").xml("<></>").build());
    assertThat(measure.getMeasureMetaData().isDraft(), is(equalTo(true)));
    String output = bundleService.bundleMeasure(measure, "Bearer TOKEN", "calculation");
    assertThat(output, is(equalTo(json)));
  }

  @Test
  void testBundleMeasureReturnsBundleStringForVersionedMeasure() {
    final String json = "{\"message\": \"GOOD JSON\"}";
    Export export = Export.builder().measureId(measure.getId()).measureBundleJson(json).build();
    measure.getMeasureMetaData().setDraft(false);
    when(exportRepository.findByMeasureId(anyString())).thenReturn(Optional.of(export));

    String output = bundleService.bundleMeasure(measure, "Bearer TOKEN", null);
    assertThat(output, is(equalTo(json)));
  }

  @Test
  void testBundleMeasureReturnsBundleStringForVersionedMeasureIfExportUnavailable() {
    measure.getMeasureMetaData().setDraft(false);
    when(exportRepository.findByMeasureId(anyString())).thenReturn(Optional.ofNullable(null));

    Exception ex =
        assertThrows(
            BundleOperationException.class,
            () -> bundleService.bundleMeasure(measure, "Bearer TOKEN", null));
    assertThat(
        ex.getMessage(),
        is(
            equalTo(
                "An error occurred while bundling Measure with ID xyz-p13r-13ert."
                    + " Please try again later or contact a System Administrator if this continues to occur.")));
  }

  @Test
  void testExportBundleMeasureForVersionedMeasure() throws IOException {

    final String json = gov.cms.madie.packaging.utils.JsonBits.BUNDLE;
    Export export = Export.builder().measureId(measure.getId()).measureBundleJson(json).build();
    measure.setEcqmTitle("MEAS");
    measure.setMeasureMetaData(
        MeasureMetaData.builder()
            .draft(false)
            .steward(Organization.builder().name("SemanticBits").build())
            .description("This is a description")
            .developers(List.of(Organization.builder().name("ICF").build()))
            .build());
    measure.setModel("QI-Core v4.1.1");
    when(exportRepository.findByMeasureId(anyString())).thenReturn(Optional.of(export));

    PackageDto output = bundleService.getMeasureExport(measure, "Bearer TOKEN");
    assertNotNull(output);
    ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(output.getExportPackage()));
    ZipEntry entry = z.getNextEntry();
    String fileName = entry.getName();
    assertEquals("resources/measure-TestCreateNewLibrary-1.0.000.json", fileName);
  }

  @Test
  void testExportBundleMeasureForVersionedMeasureDoesntExistInMongo() throws IOException {

    measure.setEcqmTitle("MEAS");
    measure.setMeasureMetaData(
        MeasureMetaData.builder()
            .draft(false)
            .steward(Organization.builder().name("SemanticBits").build())
            .description("This is a description")
            .developers(List.of(Organization.builder().name("ICF").build()))
            .build());
    measure.setModel("QI-Core v4.1.1");
    doReturn(Optional.empty()).when(exportRepository).findByMeasureId(anyString());

    Exception ex =
        assertThrows(
            BundleOperationException.class,
            () -> bundleService.getMeasureExport(measure, "Bearer TOKEN"));
    assertThat(
        ex.getMessage(),
        is(
            equalTo(
                "An error occurred while bundling Measure with ID xyz-p13r-13ert."
                    + " Please try again later or contact a System Administrator if this continues to occur.")));
  }

  @Test
  void testExportBundleMeasureForDraftMeasure() throws IOException {

    // final String json = cms.gov.madie.measure.JsonBits.BUNDLE;
    // Export export = Export.builder().measureId(measure.getId()).measureBundleJson(json).build();
    measure.setEcqmTitle("MEAS");
    measure.setMeasureMetaData(
        MeasureMetaData.builder()
            .draft(true)
            .steward(Organization.builder().name("SemanticBits").build())
            .description("This is a description")
            .developers(List.of(Organization.builder().name("ICF").build()))
            .build());
    measure.setModel("QI-Core v4.1.1");
    when(elmTranslatorClient.getElmJson(anyString(), anyString(), anyString()))
        .thenReturn(ElmJson.builder().json("{}").xml("<></>").build());
    // doThrow(new
    // HttpClientErrorException(HttpStatus.FORBIDDEN)).when(fhirServicesClient).getMeasureBundleExport(any(Measure.class), eq("")))
    byte[] exportBytes = "TEST".getBytes();
    doReturn(exportBytes)
        .when(fhirServicesClient)
        .getMeasureBundleExport(any(Measure.class), eq("Bearer TOKEN"));

    PackageDto output = bundleService.getMeasureExport(measure, "Bearer TOKEN");
    assertNotNull(output);
    assertNotNull(output);
    assertTrue(Arrays.equals("TEST".getBytes(), output.getExportPackage()));
  }

  @Test
  void testExportBundleMeasureForDraftMeasureThrowsException() throws IOException {

    measure.setEcqmTitle("MEAS");
    measure.setMeasureMetaData(
        MeasureMetaData.builder()
            .draft(true)
            .steward(Organization.builder().name("SemanticBits").build())
            .description("This is a description")
            .developers(List.of(Organization.builder().name("ICF").build()))
            .build());
    measure.setModel("QI-Core v4.1.1");
    when(elmTranslatorClient.getElmJson(anyString(), anyString(), anyString()))
        .thenReturn(ElmJson.builder().json("{}").xml("<></>").build());
    doThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN))
        .when(fhirServicesClient)
        .getMeasureBundleExport(any(Measure.class), eq("Bearer TOKEN"));

    Exception ex =
        assertThrows(
            BundleOperationException.class,
            () -> bundleService.getMeasureExport(measure, "Bearer TOKEN"));
    assertThat(
        ex.getMessage(),
        is(
            equalTo(
                "An error occurred while bundling Measure with ID xyz-p13r-13ert."
                    + " Please try again later or contact a System Administrator if this continues to occur.")));
  }

  @Test
  void testExportBundleMeasureForNullMeasureReturnsNull() throws IOException {
    PackageDto output = bundleService.getMeasureExport(null, "Bearer TOKEN");
    assertNull(output);
  }
}
