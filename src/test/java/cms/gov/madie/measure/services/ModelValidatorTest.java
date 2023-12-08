package cms.gov.madie.measure.services;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import cms.gov.madie.measure.exceptions.InvalidGroupException;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.PopulationType;
import gov.cms.madie.models.measure.Stratification;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@ActiveProfiles("test")
class ModelValidatorTest {
  @Autowired private ModelValidatorFactory modelLocator;

  @Test
  void createModelValidatorTest() {
    assertNotNull(modelLocator);
  }

  @Test
  void createQdmModelValidatorTest() {
    assertNotNull(modelLocator);

    ModelValidator validator = modelLocator.getModelValidator(ModelType.QDM_5_6);
    assertTrue(validator instanceof QdmModelValidator);
  }

  @Test
  void useQdmModelValidatorTest() {
    assertNotNull(modelLocator);
    Group group = Group.builder().stratifications(new ArrayList<Stratification>()).build();
    ModelValidator validator = modelLocator.getModelValidator(ModelType.QDM_5_6);
    assertTrue(validator instanceof QdmModelValidator);
    try {
      validator.validateGroupAssociations(group);
    } catch (Exception e) {
      fail(e);
    }
    ;
  }

  @Test
  void useQdmModelValidatorTestHasInvalidAssociation() {
    assertNotNull(modelLocator);
    Stratification strat = new Stratification();
    List<Stratification> strats = new ArrayList<>();
    strat.setAssociation(PopulationType.INITIAL_POPULATION);
    ;
    strats.add(strat);

    Group group = Group.builder().stratifications(strats).build();
    ModelValidator validator = modelLocator.getModelValidator(ModelType.QDM_5_6);
    assertTrue(validator instanceof QdmModelValidator);
    try {
      validator.validateGroupAssociations(group);
      fail("Should fail because association exists on the Stratification");
    } catch (Exception e) {
      assertEquals("QDM group stratifications cannot be associated.", e.getMessage());
    }
  }

  @Test
  void useQdmModelValidatorTestHasValidAssociation() {
    assertNotNull(modelLocator);
    Stratification strat = new Stratification();
    List<Stratification> strats = new ArrayList<>();

    strats.add(strat);

    Group group = Group.builder().stratifications(strats).build();
    ModelValidator validator = modelLocator.getModelValidator(ModelType.QDM_5_6);
    assertTrue(validator instanceof QdmModelValidator);
    try {
      validator.validateGroupAssociations(group);
    } catch (Exception e) {
      fail(e);
    }
  }

  @Test
  void createQicoreModelValidatorTest() {
    assertNotNull(modelLocator);

    ModelValidator validator = modelLocator.getModelValidator(ModelType.QI_CORE);
    assertTrue(validator instanceof QicoreModelValidator);
  }

  @Test
  void useQicoreModelValidatorTestNoStratifications() {
    assertNotNull(modelLocator);
    Group group = Group.builder().stratifications(new ArrayList<Stratification>()).build();
    ModelValidator validator = modelLocator.getModelValidator(ModelType.QI_CORE);
    assertTrue(validator instanceof QicoreModelValidator);
    try {
      validator.validateGroupAssociations(group);

    } catch (Exception e) {
      fail(e);
    }
  }

  @Test
  void useQicoreModelValidatorTestHasInvalidAssociation() {
    assertNotNull(modelLocator);
    Stratification strat = new Stratification();
    List<Stratification> strats = new ArrayList<>();

    strats.add(strat);

    Group group = Group.builder().stratifications(strats).build();
    ModelValidator validator = modelLocator.getModelValidator(ModelType.QI_CORE);
    assertTrue(validator instanceof QicoreModelValidator);
    try {
      validator.validateGroupAssociations(group);
      fail("Should fail because QICore strat association can't be null");
    } catch (InvalidGroupException e) {
      assertTrue(
          "QI-Core group stratifications should be associated to a valid population type."
              .equals(e.getMessage()));
    }
  }

  @Test
  void useQicoreModelValidatorTestHasAssociation() {
    assertNotNull(modelLocator);
    Stratification strat = new Stratification();
    List<Stratification> strats = new ArrayList<>();
    strat.setAssociation(PopulationType.INITIAL_POPULATION);
    ;
    strats.add(strat);

    Group group = Group.builder().stratifications(strats).build();
    ModelValidator validator = modelLocator.getModelValidator(ModelType.QI_CORE);
    assertTrue(validator instanceof QicoreModelValidator);
    try {
      validator.validateGroupAssociations(group);

    } catch (Exception e) {
      fail(e);
    }
  }
}
