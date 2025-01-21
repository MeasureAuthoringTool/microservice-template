package cms.gov.madie.measure.services;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import cms.gov.madie.measure.exceptions.BundleOperationException;
import cms.gov.madie.measure.exceptions.InvalidResourceStateException;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.exceptions.UnsupportedTypeException;
import cms.gov.madie.measure.factories.ModelValidatorFactory;
import cms.gov.madie.measure.repositories.ExportRepository;
import cms.gov.madie.measure.utils.MeasureUtil;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureMetaData;
import gov.cms.madie.models.measure.Export;
import gov.cms.madie.packaging.utils.PackagingUtilityFactory;
import gov.cms.madie.packaging.utils.qicore411.PackagingUtilityImpl;

@ExtendWith(MockitoExtension.class)
public class HumanReadableServiceTest {
  @Mock MeasureService measureService;
  @Mock FhirServicesClient fhirServicesClient;
  @Mock private ExportRepository exportRepository;
  @Mock private ModelValidatorFactory modelValidatorFactory;
  @Mock private QiCoreModelValidator qicoreModelValidator;
  @Mock private MeasureUtil measureUtil;

  @Captor private ArgumentCaptor<Export> exportArgumentCaptor;

  @InjectMocks HumanReadableService humanReadableService;

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
    factory = Mockito.mockStatic(PackagingUtilityFactory.class);
  }

  @AfterAll
  public static void close() {
    factory.close();
  }

  @Test
  public void testGetHumanReadableWithCSSThrowsResourceNotFoundException() {
    when(measureService.findMeasureById(anyString())).thenReturn(null);

    assertThrows(
        ResourceNotFoundException.class,
        () ->
            humanReadableService.getHumanReadableWithCSS(
                TEST_MEASURE_ID, TEST_USER, TEST_ACCESS_TOKEN));
  }

  @Test
  public void testGetHumanReadableWithCSSThrowsUnsupportedTypeException() {
    Measure existingMeasure = Measure.builder().id(TEST_MEASURE_ID).model("invalid model").build();
    when(measureService.findMeasureById(anyString())).thenReturn(existingMeasure);

    assertThrows(
        UnsupportedTypeException.class,
        () ->
            humanReadableService.getHumanReadableWithCSS(
                TEST_MEASURE_ID, TEST_USER, TEST_ACCESS_TOKEN));
  }

  @Test
  public void testGetHumanReadableThrowsInvalidResourceStateException() {
    Measure existingMeasure = Measure.builder().id(TEST_MEASURE_ID).model(MODEL_QI_CORE).build();
    when(measureService.findMeasureById(anyString())).thenReturn(existingMeasure);
    doThrow(InvalidResourceStateException.class)
        .when(measureUtil)
        .validateMetadata(any(Measure.class));
    assertThrows(
        InvalidResourceStateException.class,
        () ->
            humanReadableService.getHumanReadableWithCSS(
                TEST_MEASURE_ID, TEST_USER, TEST_ACCESS_TOKEN));
  }

  @Test
  void testGetHumanReadableThrowsInstantiationException() {
    Measure existingMeasure = Measure.builder().id(TEST_MEASURE_ID).model(MODEL_QI_CORE).build();
    when(measureService.findMeasureById(anyString())).thenReturn(existingMeasure);
    when(modelValidatorFactory.getModelValidator(any())).thenReturn(qicoreModelValidator);
    doNothing().when(measureUtil).validateMetadata(any(Measure.class));
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
                humanReadableService.getHumanReadableWithCSS(
                    TEST_MEASURE_ID, TEST_USER, TEST_ACCESS_TOKEN));
    assertThat(ex.getMessage(), is(equalTo(BUNDLE_OPERATION_ERROR)));
  }

  @Test
  void testGetHumanReadableThrowsIllegalAccessException() {
    Measure existingMeasure = Measure.builder().id(TEST_MEASURE_ID).model(MODEL_QI_CORE).build();
    when(measureService.findMeasureById(anyString())).thenReturn(existingMeasure);
    when(modelValidatorFactory.getModelValidator(any())).thenReturn(qicoreModelValidator);
    doNothing().when(measureUtil).validateMetadata(any(Measure.class));
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
                humanReadableService.getHumanReadableWithCSS(
                    TEST_MEASURE_ID, TEST_USER, TEST_ACCESS_TOKEN));
    assertThat(ex.getMessage(), is(equalTo(BUNDLE_OPERATION_ERROR)));
  }

  @Test
  void testGetHumanReadableThrowsInvocationTargetException() {
    Measure existingMeasure = Measure.builder().id(TEST_MEASURE_ID).model(MODEL_QI_CORE).build();
    when(measureService.findMeasureById(anyString())).thenReturn(existingMeasure);
    when(modelValidatorFactory.getModelValidator(any())).thenReturn(qicoreModelValidator);
    doNothing().when(measureUtil).validateMetadata(any(Measure.class));
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
                humanReadableService.getHumanReadableWithCSS(
                    TEST_MEASURE_ID, TEST_USER, TEST_ACCESS_TOKEN));
    assertThat(ex.getMessage(), is(equalTo(BUNDLE_OPERATION_ERROR)));
  }

  @Test
  public void testGetHumanReadableSuccessForQiCoreMeasureDraftStatus() {
    MeasureMetaData meta = MeasureMetaData.builder().draft(true).build();
    Measure existingMeasure =
        Measure.builder().id(TEST_MEASURE_ID).model(MODEL_QI_CORE).measureMetaData(meta).build();
    when(measureService.findMeasureById(anyString())).thenReturn(existingMeasure);
    when(modelValidatorFactory.getModelValidator(any())).thenReturn(qicoreModelValidator);
    doNothing().when(measureUtil).validateMetadata(any(Measure.class));
    when(measureUtil.validateAllMeasureDependencies(any(Measure.class)))
        .thenAnswer((invocationOnMock) -> invocationOnMock.getArgument(0));
    doNothing().when(qicoreModelValidator).validateGroups(any(Measure.class));
    doNothing().when(qicoreModelValidator).validateCqlErrors(any(Measure.class));

    when(fhirServicesClient.getMeasureBundle(any(), anyString(), anyString()))
        .thenReturn(MEASURE_BUNDLE_JSON);

    PackagingUtilityImpl utility = Mockito.mock(PackagingUtilityImpl.class);
    factory.when(() -> PackagingUtilityFactory.getInstance(MODEL_QI_CORE)).thenReturn(utility);
    when(utility.getHumanReadableWithCSS(anyString())).thenReturn(TEST_HUMAN_READABLE);

    String result =
        humanReadableService.getHumanReadableWithCSS(TEST_MEASURE_ID, TEST_USER, TEST_ACCESS_TOKEN);
    assertEquals(result, TEST_HUMAN_READABLE);
  }

  @Test
  public void testGetHRFromDBForQiCoreVersionedMeasure() {
    MeasureMetaData meta = MeasureMetaData.builder().draft(false).build();
    Measure existingMeasure =
        Measure.builder().id(TEST_MEASURE_ID).measureMetaData(meta).model(MODEL_QI_CORE).build();
    when(measureService.findMeasureById(anyString())).thenReturn(existingMeasure);

    Export export =
        Export.builder().id(TEST_ACCESS_TOKEN).humanReadable(TEST_HUMAN_READABLE).build();
    when(exportRepository.findByMeasureId(anyString())).thenReturn(Optional.of(export));

    String result =
        humanReadableService.getHumanReadableWithCSS(TEST_MEASURE_ID, TEST_USER, TEST_ACCESS_TOKEN);
    assertEquals(result, TEST_HUMAN_READABLE);
  }

  @Test
  public void testSaveHRForQiCoreVersionedMeasureNoExport() {
    MeasureMetaData meta = MeasureMetaData.builder().draft(false).build();
    Measure existingMeasure =
        Measure.builder().id(TEST_MEASURE_ID).measureMetaData(meta).model(MODEL_QI_CORE).build();
    when(measureService.findMeasureById(anyString())).thenReturn(existingMeasure);

    when(exportRepository.findByMeasureId(anyString())).thenReturn(Optional.empty());

    when(modelValidatorFactory.getModelValidator(any())).thenReturn(qicoreModelValidator);
    doNothing().when(measureUtil).validateMetadata(any(Measure.class));
    when(measureUtil.validateAllMeasureDependencies(any(Measure.class)))
        .thenAnswer((invocationOnMock) -> invocationOnMock.getArgument(0));
    doNothing().when(qicoreModelValidator).validateGroups(any(Measure.class));
    doNothing().when(qicoreModelValidator).validateCqlErrors(any(Measure.class));

    when(fhirServicesClient.getMeasureBundle(any(), anyString(), anyString()))
        .thenReturn(MEASURE_BUNDLE_JSON);

    PackagingUtilityImpl utility = Mockito.mock(PackagingUtilityImpl.class);
    factory.when(() -> PackagingUtilityFactory.getInstance(MODEL_QI_CORE)).thenReturn(utility);
    when(utility.getHumanReadableWithCSS(anyString())).thenReturn(TEST_HUMAN_READABLE);

    Export export =
        Export.builder().id(TEST_ACCESS_TOKEN).humanReadable(TEST_HUMAN_READABLE).build();
    when(exportRepository.save(any(Export.class))).thenReturn(export);

    String result =
        humanReadableService.getHumanReadableWithCSS(TEST_MEASURE_ID, TEST_USER, TEST_ACCESS_TOKEN);
    assertEquals(result, TEST_HUMAN_READABLE);
    verify(exportRepository, times(1)).save(exportArgumentCaptor.capture());
  }

  @Test
  public void testSaveHRForQiCoreVersionedMeasureNoHR() {
    MeasureMetaData meta = MeasureMetaData.builder().draft(false).build();
    Measure existingMeasure =
        Measure.builder().id(TEST_MEASURE_ID).measureMetaData(meta).model(MODEL_QI_CORE).build();
    when(measureService.findMeasureById(anyString())).thenReturn(existingMeasure);

    Export export = Export.builder().id(TEST_MEASURE_ID).build();
    when(exportRepository.findByMeasureId(anyString())).thenReturn(Optional.of(export));

    when(modelValidatorFactory.getModelValidator(any())).thenReturn(qicoreModelValidator);
    doNothing().when(measureUtil).validateMetadata(any(Measure.class));
    when(measureUtil.validateAllMeasureDependencies(any(Measure.class)))
        .thenAnswer((invocationOnMock) -> invocationOnMock.getArgument(0));
    doNothing().when(qicoreModelValidator).validateGroups(any(Measure.class));
    doNothing().when(qicoreModelValidator).validateCqlErrors(any(Measure.class));

    when(fhirServicesClient.getMeasureBundle(any(), anyString(), anyString()))
        .thenReturn(MEASURE_BUNDLE_JSON);

    PackagingUtilityImpl utility = Mockito.mock(PackagingUtilityImpl.class);
    factory.when(() -> PackagingUtilityFactory.getInstance(MODEL_QI_CORE)).thenReturn(utility);
    when(utility.getHumanReadableWithCSS(anyString())).thenReturn(TEST_HUMAN_READABLE);

    Export savedExport =
        Export.builder().id(TEST_MEASURE_ID).humanReadable(TEST_HUMAN_READABLE).build();
    when(exportRepository.save(any(Export.class))).thenReturn(savedExport);

    String result =
        humanReadableService.getHumanReadableWithCSS(TEST_MEASURE_ID, TEST_USER, TEST_ACCESS_TOKEN);
    assertEquals(result, TEST_HUMAN_READABLE);
    verify(exportRepository, times(1)).save(exportArgumentCaptor.capture());
  }
}
