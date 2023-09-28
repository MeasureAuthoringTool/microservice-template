package cms.gov.madie.measure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import cms.gov.madie.measure.repositories.MeasureRepository;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.PopulationType;
import gov.cms.madie.models.measure.Stratification;
import gov.cms.madie.models.measure.TestCase;

@ExtendWith(MockitoExtension.class)
public class UpdateStratificationAssociationTest {

  @Mock private MeasureRepository measureRepository;
  @Mock private Measure measure;
  @InjectMocks private UpdateStratificationAssociation updateStratificationAssociation;

  private Measure testMeasure;

  private List<Group> testGroups;

  @BeforeEach
  public void setUp() {
    Stratification stratification = new Stratification() ; 
    stratification.setAssociation(PopulationType.INITIAL_POPULATION);
    List<Group> groups = List.of(Group.builder().stratifications(List.of(stratification)).build());
    testMeasure = Measure.builder().id("testMeasureId").model(ModelType.QDM_5_6.getValue()).groups(groups).build();
  }

  @Test
  public void addPatientIdChangeUnitSuccess() {
    doReturn(List.of(testMeasure)).when(measureRepository).findAllByModel(ModelType.QDM_5_6.getValue());
    updateStratificationAssociation.removeAssociationFromStratification(measureRepository);

    Stratification strat = testMeasure.getGroups().get(0).getStratifications().get(0);
    assertNotNull(strat);
    
    assertNull(strat.getAssociation());
    
  }



  @Test
  public void testRollbackExecutionHasMeasures() {
    ReflectionTestUtils.setField(updateStratificationAssociation, "tempMeasures", List.of(testMeasure));

    updateStratificationAssociation.rollbackExecution(measureRepository);

    Stratification strat = testMeasure.getGroups().get(0).getStratifications().get(0);
    assertNotNull(strat);
    
    assertEquals(PopulationType.INITIAL_POPULATION, strat.getAssociation());
  }

}
