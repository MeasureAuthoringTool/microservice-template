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
public class MigrateRiskAdjustmentChangeUnitTest {

  @Mock MeasureRepository measureRepository;

  @InjectMocks MigrateRiskAdjustmentChangeUnit changeUnit;

  private Measure measure;

  @Captor private ArgumentCaptor<Measure> measureArgumentCaptor;

  @BeforeEach
  public void setup() {
    MeasureMetaData metaData =
        MeasureMetaData.builder().riskAdjustment("test risk adjustment").build();
    DefDescPair rav =
        DefDescPair.builder().definition("RAV definition").description("RAV description").build();
    measure =
        Measure.builder()
            .id("TestMeasure1")
            .measureName("Test Measure 1")
            .model(ModelType.QI_CORE.getValue())
            .riskAdjustments(List.of(rav))
            .measureMetaData(metaData)
            .build();
  }

  @Test
  public void testChangeUnitExecutionEmptyRepository() throws Exception {
    when(measureRepository.findAll()).thenReturn(List.of());

    changeUnit.migrateRiskAdjustment(measureRepository);

    verifyNoMoreInteractions(measureRepository);
  }

  @Test
  public void testChangeUnitExecutionMeasureHasNoRiskAdjustment() throws Exception {
    measure.setRiskAdjustments(null);

    when(measureRepository.findAll()).thenReturn(List.of(measure));

    changeUnit.migrateRiskAdjustment(measureRepository);

    verifyNoMoreInteractions(measureRepository);
  }

  @Test
  public void testChangeUnitExecutionQdmMeasure() throws Exception {
    measure.setModel(ModelType.QDM_5_6.getValue());

    when(measureRepository.findAll()).thenReturn(List.of(measure));

    changeUnit.migrateRiskAdjustment(measureRepository);

    verifyNoMoreInteractions(measureRepository);
  }

  @Test
  public void testChangeUnitExecutionRiskAdjustmentsMigrated() throws Exception {
    when(measureRepository.findAll()).thenReturn(List.of(measure));

    changeUnit.migrateRiskAdjustment(measureRepository);

    verify(measureRepository, times(1)).save(measureArgumentCaptor.capture());
    Measure measure = measureArgumentCaptor.getValue();

    assertThat(measure.getRiskAdjustments().size(), is(equalTo(1)));
    assertThat(
        measure.getRiskAdjustmentDescription(),
        is(equalTo("test risk adjustment; RAV definition-RAV description \n ")));
  }

  @Test
  public void testChangeUnitExecutionRAVMigratedNoDescription() throws Exception {
    DefDescPair rav = DefDescPair.builder().definition("RAV definition").build();
    measure.setRiskAdjustments(List.of(rav));

    when(measureRepository.findAll()).thenReturn(List.of(measure));

    changeUnit.migrateRiskAdjustment(measureRepository);

    verify(measureRepository, times(1)).save(measureArgumentCaptor.capture());
    Measure measure = measureArgumentCaptor.getValue();

    assertThat(measure.getRiskAdjustments().size(), is(equalTo(1)));
    assertThat(
        measure.getRiskAdjustmentDescription(),
        is(equalTo("test risk adjustment; RAV definition \n ")));
  }

  @Test
  public void testChangeUnitExecutionRAVMigratedNoMetaData() throws Exception {
    measure.setMeasureMetaData(null);

    when(measureRepository.findAll()).thenReturn(List.of(measure));

    changeUnit.migrateRiskAdjustment(measureRepository);

    verify(measureRepository, times(1)).save(measureArgumentCaptor.capture());
    Measure measure = measureArgumentCaptor.getValue();

    assertThat(measure.getRiskAdjustments().size(), is(equalTo(1)));
    assertThat(
        measure.getRiskAdjustmentDescription(), is(equalTo("RAV definition-RAV description \n ")));
  }

  @Test
  public void testChangeUnitExecutionRAVMigratedRAVFromMetaDataNull() throws Exception {
    measure.getMeasureMetaData().setRiskAdjustment(null);

    when(measureRepository.findAll()).thenReturn(List.of(measure));

    changeUnit.migrateRiskAdjustment(measureRepository);

    verify(measureRepository, times(1)).save(measureArgumentCaptor.capture());
    Measure measure = measureArgumentCaptor.getValue();

    assertThat(measure.getRiskAdjustments().size(), is(equalTo(1)));
    assertThat(
        measure.getRiskAdjustmentDescription(), is(equalTo("RAV definition-RAV description \n ")));
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
    verify(measureRepository, times(1)).saveAll(any(List.class));
  }
}
