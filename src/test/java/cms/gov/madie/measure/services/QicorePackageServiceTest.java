package cms.gov.madie.measure.services;

import cms.gov.madie.measure.dto.PackageDto;
import cms.gov.madie.measure.dto.qrda.QrdaRequestDTO;
import cms.gov.madie.measure.exceptions.BundleOperationException;
import cms.gov.madie.measure.exceptions.InvalidResourceStateException;
import cms.gov.madie.measure.factories.ModelValidatorFactory;
import cms.gov.madie.measure.repositories.ExportRepository;
import cms.gov.madie.measure.utils.MeasureUtil;
import gov.cms.madie.models.measure.Export;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureMetaData;
import gov.cms.madie.packaging.utils.PackagingUtilityFactory;
import gov.cms.madie.packaging.utils.qicore411.PackagingUtilityImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
public class QicorePackageServiceTest {
  @Mock private BundleService bundleService;
  @Mock MeasureService measureService;
  @Mock FhirServicesClient fhirServicesClient;
  @Mock private ExportRepository exportRepository;
  @Mock private ModelValidatorFactory modelValidatorFactory;
  @Mock private QiCoreModelValidator qicoreModelValidator;
  @Mock private MeasureUtil measureUtil;

  @Captor private ArgumentCaptor<Export> exportArgumentCaptor;

  @InjectMocks private QicorePackageService qicorePackageService;

  private Measure existingMeasure;
  private static final String TEST_USER = "test-user";
  private static final String TEST_ACCESS_TOKEN = "test-access-token";
  private static final String TEST_MEASURE_ID = "testMeasureId";
  private static final String MODEL_QI_CORE = "QI-Core v4.1.1";
  private static final String MEASURE_BUNDLE_JSON =
      """
              {"resourceType": "Bundle","entry": [ {
                  "resource": {
                    "resourceType": "Measure","text":{"div":"humanReadable"}}}]}""";
  private static final String TEST_HUMAN_READABLE = "test human readable";
  private static final String BUNDLE_OPERATION_ERROR =
      "An error occurred while bundling Measure with ID testMeasureId. Please try again later or contact a System Administrator if this continues to occur.";

  private static MockedStatic<PackagingUtilityFactory> factory;

  @BeforeAll
  public static void staticSetup() {
    factory = mockStatic(PackagingUtilityFactory.class);
  }

  @BeforeEach
  public void setup() {
    existingMeasure = Measure.builder().id(TEST_MEASURE_ID).model(MODEL_QI_CORE).build();
  }

  @AfterAll
  public static void close() {
    factory.close();
  }

  @Test
  void getMeasurePackage() {
    String measurePackageStr = "measure package";
    PackageDto packageDto =
        PackageDto.builder().fromStorage(false).exportPackage(measurePackageStr.getBytes()).build();
    when(bundleService.getMeasureExport(any(Measure.class), anyString())).thenReturn(packageDto);
    PackageDto measurePackage = qicorePackageService.getMeasurePackage(new Measure(), "token");
    byte[] rawPackage = measurePackage.getExportPackage();
    assertThat(new String(rawPackage), is(equalTo(measurePackageStr)));
  }

  @Test
  void testGetQRDA() {
    Exception ex =
        assertThrows(
            UnsupportedOperationException.class,
            () ->
                qicorePackageService.getQRDA(
                    QrdaRequestDTO.builder().measure(new Measure()).build(), "token"));
    assertThat(ex.getMessage(), containsString("method not yet implemented"));
  }

  @Test
  public void testGetHumanReadableThrowsInvalidResourceStateException() {
    when(modelValidatorFactory.getModelValidator(any())).thenReturn(qicoreModelValidator);
    doThrow(InvalidResourceStateException.class)
        .when(qicoreModelValidator)
        .validateMetadata(any(Measure.class));
    assertThrows(
        InvalidResourceStateException.class,
        () -> qicorePackageService.getHumanReadable(existingMeasure, TEST_USER, TEST_ACCESS_TOKEN));
  }

  @Test
  void testGetHumanReadableThrowsInstantiationException() {
    when(modelValidatorFactory.getModelValidator(any())).thenReturn(qicoreModelValidator);
    doNothing().when(qicoreModelValidator).validateMetadata(any(Measure.class));
    when(measureUtil.validateAllMeasureDependencies(any(Measure.class)))
        .thenAnswer((invocationOnMock) -> invocationOnMock.getArgument(0));
    doNothing().when(qicoreModelValidator).validateGroups(any(Measure.class));
    doNothing().when(qicoreModelValidator).validateCqlErrors(any(Measure.class));

    when(fhirServicesClient.getMeasureBundle(any(), anyString(), anyString()))
        .thenReturn(MEASURE_BUNDLE_JSON);

    factory
        .when(() -> PackagingUtilityFactory.getInstance(MODEL_QI_CORE))
        .thenThrow(
            new InstantiationException("Unexpected error while getting human readable with CSS"));

    Exception ex =
        assertThrows(
            BundleOperationException.class,
            () ->
                qicorePackageService.getHumanReadable(
                    existingMeasure, TEST_USER, TEST_ACCESS_TOKEN));
    assertThat(ex.getMessage(), is(equalTo(BUNDLE_OPERATION_ERROR)));
  }

  @Test
  void testGetHumanReadableThrowsIllegalAccessException() {
    when(modelValidatorFactory.getModelValidator(any())).thenReturn(qicoreModelValidator);
    doNothing().when(qicoreModelValidator).validateMetadata(any(Measure.class));
    when(measureUtil.validateAllMeasureDependencies(any(Measure.class)))
        .thenAnswer((invocationOnMock) -> invocationOnMock.getArgument(0));
    doNothing().when(qicoreModelValidator).validateGroups(any(Measure.class));
    doNothing().when(qicoreModelValidator).validateCqlErrors(any(Measure.class));

    when(fhirServicesClient.getMeasureBundle(any(), anyString(), anyString()))
        .thenReturn(MEASURE_BUNDLE_JSON);

    factory
        .when(() -> PackagingUtilityFactory.getInstance(MODEL_QI_CORE))
        .thenThrow(
            new IllegalAccessException("Unexpected error while getting human readable with CSS"));

    Exception ex =
        assertThrows(
            BundleOperationException.class,
            () ->
                qicorePackageService.getHumanReadable(
                    existingMeasure, TEST_USER, TEST_ACCESS_TOKEN));
    assertThat(ex.getMessage(), is(equalTo(BUNDLE_OPERATION_ERROR)));
  }

  @Test
  void testGetHumanReadableThrowsInvocationTargetException() {
    when(modelValidatorFactory.getModelValidator(any())).thenReturn(qicoreModelValidator);
    doNothing().when(qicoreModelValidator).validateMetadata(any(Measure.class));
    when(measureUtil.validateAllMeasureDependencies(any(Measure.class)))
        .thenAnswer((invocationOnMock) -> invocationOnMock.getArgument(0));
    doNothing().when(qicoreModelValidator).validateGroups(any(Measure.class));
    doNothing().when(qicoreModelValidator).validateCqlErrors(any(Measure.class));

    when(fhirServicesClient.getMeasureBundle(any(), anyString(), anyString()))
        .thenReturn(MEASURE_BUNDLE_JSON);

    factory
        .when(() -> PackagingUtilityFactory.getInstance(MODEL_QI_CORE))
        .thenThrow(
            new InvocationTargetException(
                new Throwable("Unexpected error while getting human readable with CSS")));

    Exception ex =
        assertThrows(
            BundleOperationException.class,
            () ->
                qicorePackageService.getHumanReadable(
                    existingMeasure, TEST_USER, TEST_ACCESS_TOKEN));
    assertThat(ex.getMessage(), is(equalTo(BUNDLE_OPERATION_ERROR)));
  }

  @Test
  public void testGetHumanReadableSuccessForQiCoreMeasureDraftStatus() {
    MeasureMetaData meta = MeasureMetaData.builder().draft(true).build();
    existingMeasure.setMeasureMetaData(meta);
    when(modelValidatorFactory.getModelValidator(any())).thenReturn(qicoreModelValidator);
    doNothing().when(qicoreModelValidator).validateMetadata(any(Measure.class));
    when(measureUtil.validateAllMeasureDependencies(any(Measure.class)))
        .thenAnswer((invocationOnMock) -> invocationOnMock.getArgument(0));
    doNothing().when(qicoreModelValidator).validateGroups(any(Measure.class));
    doNothing().when(qicoreModelValidator).validateCqlErrors(any(Measure.class));

    when(fhirServicesClient.getMeasureBundle(any(), anyString(), anyString()))
        .thenReturn(MEASURE_BUNDLE_JSON);

    PackagingUtilityImpl utility = mock(PackagingUtilityImpl.class);
    factory.when(() -> PackagingUtilityFactory.getInstance(MODEL_QI_CORE)).thenReturn(utility);
    when(utility.getHumanReadableWithCSS(anyString())).thenReturn(TEST_HUMAN_READABLE);

    String result =
        qicorePackageService.getHumanReadable(existingMeasure, TEST_USER, TEST_ACCESS_TOKEN);
    assertEquals(result, TEST_HUMAN_READABLE);
  }

  @Test
  public void testGetHRFromDBForQiCoreVersionedMeasure() {
    MeasureMetaData meta = MeasureMetaData.builder().draft(false).build();
    existingMeasure.setMeasureMetaData(meta);

    Export export =
        Export.builder().id(TEST_ACCESS_TOKEN).humanReadable(TEST_HUMAN_READABLE).build();
    when(exportRepository.findByMeasureId(anyString())).thenReturn(Optional.of(export));

    String result =
        qicorePackageService.getHumanReadable(existingMeasure, TEST_USER, TEST_ACCESS_TOKEN);
    assertEquals(result, TEST_HUMAN_READABLE);
  }

  @Test
  public void testSaveHRForQiCoreVersionedMeasureNoExport() {
    MeasureMetaData meta = MeasureMetaData.builder().draft(false).build();
    existingMeasure.setMeasureMetaData(meta);

    when(exportRepository.findByMeasureId(anyString())).thenReturn(Optional.empty());

    when(modelValidatorFactory.getModelValidator(any())).thenReturn(qicoreModelValidator);
    doNothing().when(qicoreModelValidator).validateMetadata(any(Measure.class));
    when(measureUtil.validateAllMeasureDependencies(any(Measure.class)))
        .thenAnswer((invocationOnMock) -> invocationOnMock.getArgument(0));
    doNothing().when(qicoreModelValidator).validateGroups(any(Measure.class));
    doNothing().when(qicoreModelValidator).validateCqlErrors(any(Measure.class));

    when(fhirServicesClient.getMeasureBundle(any(), anyString(), anyString()))
        .thenReturn(MEASURE_BUNDLE_JSON);

    PackagingUtilityImpl utility = mock(PackagingUtilityImpl.class);
    factory.when(() -> PackagingUtilityFactory.getInstance(MODEL_QI_CORE)).thenReturn(utility);
    when(utility.getHumanReadableWithCSS(anyString())).thenReturn(TEST_HUMAN_READABLE);

    Export savedExport =
        Export.builder().id(TEST_MEASURE_ID).humanReadable(TEST_HUMAN_READABLE).build();
    when(exportRepository.save(any(Export.class))).thenReturn(savedExport);

    String result =
        qicorePackageService.getHumanReadable(existingMeasure, TEST_USER, TEST_ACCESS_TOKEN);
    assertEquals(result, TEST_HUMAN_READABLE);
    verify(exportRepository, times(1)).save(exportArgumentCaptor.capture());
  }

  @Test
  public void testSaveHRForQiCoreVersionedMeasureNoHR() {
    MeasureMetaData meta = MeasureMetaData.builder().draft(false).build();
    existingMeasure.setMeasureMetaData(meta);

    Export export = Export.builder().id(TEST_MEASURE_ID).build();
    when(exportRepository.findByMeasureId(anyString())).thenReturn(Optional.of(export));

    when(modelValidatorFactory.getModelValidator(any())).thenReturn(qicoreModelValidator);
    doNothing().when(qicoreModelValidator).validateMetadata(any(Measure.class));
    when(measureUtil.validateAllMeasureDependencies(any(Measure.class)))
        .thenAnswer((invocationOnMock) -> invocationOnMock.getArgument(0));
    doNothing().when(qicoreModelValidator).validateGroups(any(Measure.class));
    doNothing().when(qicoreModelValidator).validateCqlErrors(any(Measure.class));

    when(fhirServicesClient.getMeasureBundle(any(), anyString(), anyString()))
        .thenReturn(MEASURE_BUNDLE_JSON);

    PackagingUtilityImpl utility = mock(PackagingUtilityImpl.class);
    factory.when(() -> PackagingUtilityFactory.getInstance(MODEL_QI_CORE)).thenReturn(utility);
    when(utility.getHumanReadableWithCSS(anyString())).thenReturn(TEST_HUMAN_READABLE);

    Export savedExport =
        Export.builder().id(TEST_MEASURE_ID).humanReadable(TEST_HUMAN_READABLE).build();
    when(exportRepository.save(any(Export.class))).thenReturn(savedExport);

    String result =
        qicorePackageService.getHumanReadable(existingMeasure, TEST_USER, TEST_ACCESS_TOKEN);
    assertEquals(result, TEST_HUMAN_READABLE);
    verify(exportRepository, times(1)).save(exportArgumentCaptor.capture());
  }
}
