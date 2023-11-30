package cms.gov.madie.measure.config;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import cms.gov.madie.measure.repositories.MeasureRepository;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.measure.DefDescPair;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureMetaData;

@ExtendWith(MockitoExtension.class)
public class MigrateSupplementalDataElementsChangeUnitTest {

  @Mock MeasureRepository measureRepository;

  @InjectMocks MigrateSupplementalDataElementsChangeUnit changeUnit;

  private Measure measure;

  @Captor private ArgumentCaptor<Measure> measureArgumentCaptor;

  @BeforeEach
  public void setup() {
    MeasureMetaData metaData =
        MeasureMetaData.builder().supplementalDataElements("test supplemental data").build();
    DefDescPair supplementalData =
        DefDescPair.builder().definition("SDE definition").description("SDE description").build();
    measure =
        Measure.builder()
            .id("TestMeasure1")
            .measureName("Test Measure 1")
            .model(ModelType.QI_CORE.getValue())
            .supplementalData(List.of(supplementalData))
            .measureMetaData(metaData)
            .build();
  }

  @Test
  public void testChangeUnitExecutionEmptyRepository() throws Exception {
    when(measureRepository.findAll()).thenReturn(List.of());

    changeUnit.migrateSupplementalDataElements(measureRepository);

    verifyNoMoreInteractions(measureRepository);
  }

  @Test
  public void testChangeUnitExecutionMeasureHasNoSDE() throws Exception {
    measure.setSupplementalData(null);

    when(measureRepository.findAll()).thenReturn(List.of(measure));

    changeUnit.migrateSupplementalDataElements(measureRepository);

    verifyNoMoreInteractions(measureRepository);
  }

  @Test
  public void testChangeUnitExecutionQdmMeasure() throws Exception {
    measure.setModel(ModelType.QDM_5_6.getValue());

    when(measureRepository.findAll()).thenReturn(List.of(measure));

    changeUnit.migrateSupplementalDataElements(measureRepository);

    verifyNoMoreInteractions(measureRepository);
  }

  @Test
  public void testChangeUnitExecutionSupplementalDataElementsMigrated() throws Exception {
    when(measureRepository.findAll()).thenReturn(List.of(measure));

    changeUnit.migrateSupplementalDataElements(measureRepository);

    verify(measureRepository, times(1)).save(measureArgumentCaptor.capture());
    Measure measure = measureArgumentCaptor.getValue();

    assertThat(measure.getSupplementalData().size(), is(equalTo(1)));
    assertThat(
        measure.getSupplementalDataDescription(),
        is(equalTo("test supplemental data; SDE definition-SDE description | ")));
  }

  @Test
  public void testChangeUnitExecutionSDEMigratedNoDescription() throws Exception {
    DefDescPair supplementalData = DefDescPair.builder().definition("SDE definition").build();
    measure.setSupplementalData(List.of(supplementalData));

    when(measureRepository.findAll()).thenReturn(List.of(measure));

    changeUnit.migrateSupplementalDataElements(measureRepository);

    verify(measureRepository, times(1)).save(measureArgumentCaptor.capture());
    Measure measure = measureArgumentCaptor.getValue();

    assertThat(measure.getSupplementalData().size(), is(equalTo(1)));
    assertThat(
        measure.getSupplementalDataDescription(),
        is(equalTo("test supplemental data; SDE definition | ")));
  }

  @Test
  public void testChangeUnitExecutionSDEMigratedNoMetaData() throws Exception {
    measure.setMeasureMetaData(null);

    when(measureRepository.findAll()).thenReturn(List.of(measure));

    changeUnit.migrateSupplementalDataElements(measureRepository);

    verify(measureRepository, times(1)).save(measureArgumentCaptor.capture());
    Measure measure = measureArgumentCaptor.getValue();

    assertThat(measure.getSupplementalData().size(), is(equalTo(1)));
    assertThat(
        measure.getSupplementalDataDescription(), is(equalTo("SDE definition-SDE description | ")));
  }

  @Test
  public void testChangeUnitExecutionSDEMigratedSDEFromMetaDataNull() throws Exception {
    measure.getMeasureMetaData().setSupplementalDataElements(null);

    when(measureRepository.findAll()).thenReturn(List.of(measure));

    changeUnit.migrateSupplementalDataElements(measureRepository);

    verify(measureRepository, times(1)).save(measureArgumentCaptor.capture());
    Measure measure = measureArgumentCaptor.getValue();

    assertThat(measure.getSupplementalData().size(), is(equalTo(1)));
    assertThat(
        measure.getSupplementalDataDescription(), is(equalTo("SDE definition-SDE description | ")));
  }

  @Test
  public void testRollBackNoMeasures() throws Exception {
    changeUnit.rollbackExecution(measureRepository);
    verifyNoInteractions(measureRepository);
  }

  @Test
  public void testRollBackMultipleMeasures() throws Exception {
    ReflectionTestUtils.setField(changeUnit, "tempMeasures", List.of(measure, measure));
    changeUnit.rollbackExecution(measureRepository);
    verify(measureRepository, times(2)).save(any(Measure.class));
  }
}
