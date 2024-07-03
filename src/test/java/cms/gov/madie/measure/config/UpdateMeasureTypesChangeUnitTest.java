package cms.gov.madie.measure.config;

import cms.gov.madie.measure.repositories.MeasureRepository;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.measure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
public class UpdateMeasureTypesChangeUnitTest {

  @Mock private MeasureRepository measureRepository;
  @InjectMocks private UpdateMeasureTypesChangeUnit changeUnit;

  private QdmMeasure qdmMeasure;

  @Captor private ArgumentCaptor<QdmMeasure> measureArgumentCaptor;

  @BeforeEach
  public void setUp() {
    qdmMeasure =
        QdmMeasure.builder()
            .active(true)
            .id("xyz-p13r-13ert")
            .model(ModelType.QDM_5_6.getValue())
            .baseConfigurationTypes(
                List.of(
                    BaseConfigurationTypes.PATIENT_ENGAGEMENT_OR_EXPERIENCE,
                    BaseConfigurationTypes.COST_OR_RESOURCE_USE))
            .cql("test cql")
            .elmJson("")
            .measureSetId("IDIDID")
            .cqlLibraryName("MSR01Library")
            .measureName("MSR01")
            .measureMetaData(new MeasureMetaData().toBuilder().build())
            .version(new Version(0, 0, 1))
            .groups(List.of())
            .createdAt(Instant.now())
            .createdBy("test user")
            .lastModifiedAt(Instant.now())
            .lastModifiedBy("test user")
            .build();
  }

  @Test
  void updateMeasureTypesHandlesEmptyMeasuresCollection() {

    when(measureRepository.findAll()).thenReturn(List.of());
    changeUnit.updateMeasureTypes(measureRepository);

    verify(measureRepository, times(1)).findAll();
    verifyNoMoreInteractions(measureRepository);
  }

  @Test
  void updateMeasureTypesHandlesNullMeasuresCollection() {

    when(measureRepository.findAll()).thenReturn(null);
    changeUnit.updateMeasureTypes(measureRepository);

    verify(measureRepository, times(1)).findAll();
    verifyNoMoreInteractions(measureRepository);
  }

  @Test
  void updateMeasureTypesHandlesEmptyMeasureTypesCollection() {

    qdmMeasure.setBaseConfigurationTypes(List.of());

    when(measureRepository.findAll()).thenReturn(List.of(qdmMeasure));
    changeUnit.updateMeasureTypes(measureRepository);

    verify(measureRepository, times(1)).findAll();
    verifyNoMoreInteractions(measureRepository);
  }

  @Test
  void updateMeasureTypes() {

    when(measureRepository.findAll()).thenReturn(List.of(qdmMeasure));
    changeUnit.updateMeasureTypes(measureRepository);

    verify(measureRepository, times(1)).findAll();
    verify(measureRepository, times(1)).save(measureArgumentCaptor.capture());
    QdmMeasure updatedMeasure = measureArgumentCaptor.getValue();
    assertThat(updatedMeasure, is(notNullValue()));
    assertThat(updatedMeasure.getBaseConfigurationTypes(), is(notNullValue()));
    assertTrue(
        updatedMeasure.getBaseConfigurationTypes().contains(BaseConfigurationTypes.RESOURCE_USE));
    assertTrue(
        updatedMeasure.getBaseConfigurationTypes().contains(BaseConfigurationTypes.EXPERIENCE));
  }

  @Test
  void rollbackExecutionDoesNothing() {
    changeUnit.rollbackExecution();
    verifyNoInteractions(measureRepository);
  }
}
