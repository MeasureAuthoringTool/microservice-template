package cms.gov.madie.measure.services;

import cms.gov.madie.measure.dto.PackageDto;
import cms.gov.madie.measure.dto.qrda.QrdaRequestDTO;
import cms.gov.madie.measure.exceptions.InvalidResourceStateException;
import cms.gov.madie.measure.factories.ModelValidatorFactory;
import cms.gov.madie.measure.factories.PackageServiceFactory;
import cms.gov.madie.measure.utils.MeasureUtil;
import gov.cms.madie.models.common.Organization;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureGroupTypes;
import gov.cms.madie.models.measure.MeasureMetaData;
import gov.cms.madie.models.measure.MeasureSet;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.PopulationType;
import gov.cms.madie.models.measure.TestCase;
import gov.cms.madie.models.validators.ValidLibraryNameValidator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class ExportServiceTest {
  @Mock private PackageServiceFactory packageServiceFactory;
  @Mock private ModelValidatorFactory modelValidatorFactory;
  @Mock private QicorePackageService qicorePackageService;
  @Mock private QdmPackageService qdmPackageService;
  @Mock private QiCoreModelValidator qicoreModelValidator;
  @Mock private QdmModelValidator qdmModelValidator;
  @Mock private ValidLibraryNameValidator validLibraryNameValidator;
  @Mock private MeasureUtil measureUtil;
  @InjectMocks ExportService exportService;

  private final String packageContent = "raw package";
  private final String token = "token";
  private Measure measure;
  private MeasureMetaData measureMetaData;

  @BeforeEach
  void setup() {
    Group group =
        Group.builder()
            .scoring("Cohort")
            .populationBasis("Encounter")
            .measureGroupTypes(Arrays.asList(MeasureGroupTypes.OUTCOME))
            .populations(
                List.of(
                    new Population(
                        "id-1",
                        PopulationType.INITIAL_POPULATION,
                        "Initial Population",
                        null,
                        null)))
            .groupDescription("Description")
            .scoringUnit("test-scoring-unit")
            .build();
    measure =
        Measure.builder()
            .id("measure-id")
            .createdBy("test.user")
            .groups(List.of(group))
            .measureMetaData(MeasureMetaData.builder().draft(true).build())
            .measureSet(MeasureSet.builder().owner("test.user").build())
            .build();
    measureMetaData =
        MeasureMetaData.builder()
            .draft(false)
            .steward(Organization.builder().name("SemanticBits").build())
            .description("This is a description")
            .developers(List.of(Organization.builder().name("ICF").build()))
            .build();
    measure.setMeasureMetaData(measureMetaData);
    TestCase testCase = TestCase.builder().build();
    measure.setTestCases(List.of(testCase));
  }

  @Test
  void testGetQdmMeasurePackage() {
    when(modelValidatorFactory.getModelValidator(any())).thenReturn(qdmModelValidator);
    doNothing().when(qdmModelValidator).validateGroups(any(Measure.class));
    when(measureUtil.validateAllMeasureDependencies(any(Measure.class)))
        .thenAnswer((invocationOnMock) -> invocationOnMock.getArgument(0));
    when(packageServiceFactory.getPackageService(any())).thenReturn(qdmPackageService);
    when(qdmPackageService.getMeasurePackage(any(Measure.class), anyString()))
        .thenReturn(
            PackageDto.builder()
                .fromStorage(false)
                .exportPackage(packageContent.getBytes())
                .build());
    PackageDto output = exportService.getMeasureExport(measure, token);
    byte[] measurePackage = output.getExportPackage();
    assertThat(new String(measurePackage), is(equalTo(packageContent)));
  }

  @Test
  void testGetQiCoreMeasurePackage() {
    when(modelValidatorFactory.getModelValidator(any())).thenReturn(qicoreModelValidator);
    doNothing().when(qicoreModelValidator).validateGroups(any(Measure.class));
    when(measureUtil.validateAllMeasureDependencies(any(Measure.class)))
        .thenAnswer((invocationOnMock) -> invocationOnMock.getArgument(0));
    when(packageServiceFactory.getPackageService(any())).thenReturn(qicorePackageService);
    when(qicorePackageService.getMeasurePackage(any(Measure.class), anyString()))
        .thenReturn(
            PackageDto.builder()
                .fromStorage(false)
                .exportPackage(packageContent.getBytes())
                .build());
    PackageDto output = exportService.getMeasureExport(measure, token);
    byte[] measurePackage = output.getExportPackage();
    assertThat(new String(measurePackage), is(equalTo(packageContent)));
  }

  @Test
  void testGetMeasurePackageWhenNoDevelopers() {
    measureMetaData.setDevelopers(List.of());
    Exception ex =
        Assertions.assertThrows(
            InvalidResourceStateException.class,
            () -> exportService.getMeasureExport(measure, token));
    assertThat(
        ex.getMessage(),
        is(
            equalTo(
                "Response could not be completed for Measure with ID measure-id, since there are no associated developers in metadata.")));
  }

  @Test
  void testGetMeasurePackageWhenNoStewards() {
    measureMetaData.setSteward(null);
    Exception ex =
        Assertions.assertThrows(
            InvalidResourceStateException.class,
            () -> exportService.getMeasureExport(measure, token));
    assertThat(
        ex.getMessage(),
        is(
            equalTo(
                "Response could not be completed for Measure with ID measure-id, since there is no associated steward in metadata.")));
  }

  @Test
  void testGetMeasurePackageWhenNoMeasureDescription() {
    measureMetaData.setDescription(null);
    Exception ex =
        Assertions.assertThrows(
            InvalidResourceStateException.class,
            () -> exportService.getMeasureExport(measure, token));
    assertThat(
        ex.getMessage(),
        is(
            equalTo(
                "Response could not be completed for Measure with ID measure-id, since there is no description in metadata.")));
  }

  @Test
  void testGetMeasurePackageWhenMetaDataIsNull() {
    measure.setMeasureMetaData(null);
    when(modelValidatorFactory.getModelValidator(any())).thenReturn(qdmModelValidator);
    doNothing().when(qdmModelValidator).validateGroups(any(Measure.class));
    when(measureUtil.validateAllMeasureDependencies(any(Measure.class)))
        .thenAnswer((invocationOnMock) -> invocationOnMock.getArgument(0));
    when(packageServiceFactory.getPackageService(any())).thenReturn(qdmPackageService);
    PackageDto packageDto =
        PackageDto.builder().fromStorage(false).exportPackage(packageContent.getBytes()).build();
    when(qdmPackageService.getMeasurePackage(any(Measure.class), anyString()))
        .thenReturn(packageDto);
    PackageDto output = exportService.getMeasureExport(measure, token);
    byte[] measurePackage = output.getExportPackage();
    assertThat(new String(measurePackage), is(equalTo(packageContent)));
  }

  @Test
  void testGetQRDA() {
    when(packageServiceFactory.getPackageService(any())).thenReturn(qdmPackageService);
    when(qdmPackageService.getQRDA(any(QrdaRequestDTO.class), anyString()))
        .thenReturn(new ResponseEntity<>(packageContent.getBytes(), HttpStatus.OK));
    ResponseEntity<byte[]> measurePackage =
        exportService.getQRDA(QrdaRequestDTO.builder().measure(measure).build(), token);
    assertThat(new String(measurePackage.getBody()), is(equalTo(packageContent)));
  }

  @Test
  void testGetQRDANoTestCases() {
    measure.setTestCases(Collections.emptyList());
    Exception ex =
        Assertions.assertThrows(
            InvalidResourceStateException.class,
            () -> exportService.getQRDA(QrdaRequestDTO.builder().measure(measure).build(), token));
    assertThat(
        ex.getMessage(),
        is(
            equalTo(
                "Response could not be completed for Measure with ID measure-id, since there are no test cases in the measure.")));
  }
}
