package cms.gov.madie.measure.services;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cms.gov.madie.measure.exceptions.InvalidResourceStateException;
import cms.gov.madie.measure.factories.ModelValidatorFactory;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureErrorType;

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
  @Autowired private ModelValidatorFactory modelValidatorFactory;

  @Test
  void createModelValidatorTest() {
    assertNotNull(modelValidatorFactory);
  }

  @Test
  void createQdmModelValidatorTest() {
    assertNotNull(modelValidatorFactory);

    ModelValidator validator = modelValidatorFactory.getModelValidator(ModelType.QDM_5_6);
    assertTrue(validator instanceof QdmModelValidator);
  }

  @Test
  void useQdmModelValidatorTest() {
    assertNotNull(modelValidatorFactory);
    Group group = Group.builder().stratifications(new ArrayList<Stratification>()).build();
    ModelValidator validator = modelValidatorFactory.getModelValidator(ModelType.QDM_5_6);
    assertTrue(validator instanceof QdmModelValidator);
    try {
      validator.validateGroupAssociations(group);
    } catch (Exception e) {
      fail(e);
    }
  }

  @Test
  void useQdmModelValidatorTestHasInvalidAssociation() {
    assertNotNull(modelValidatorFactory);
    Stratification strat = new Stratification();
    List<Stratification> strats = new ArrayList<>();
    strat.setAssociation(PopulationType.INITIAL_POPULATION);
    strats.add(strat);

    Group group = Group.builder().stratifications(strats).build();
    ModelValidator validator = modelValidatorFactory.getModelValidator(ModelType.QDM_5_6);
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
    assertNotNull(modelValidatorFactory);
    Stratification strat = new Stratification();
    List<Stratification> strats = new ArrayList<>();

    strats.add(strat);

    Group group = Group.builder().stratifications(strats).build();
    ModelValidator validator = modelValidatorFactory.getModelValidator(ModelType.QDM_5_6);
    assertTrue(validator instanceof QdmModelValidator);
    try {
      validator.validateGroupAssociations(group);
    } catch (Exception e) {
      fail(e);
    }
  }

  @Test
  void useQdmModelValidatorTestMeasureHasNoGroup() {
    assertNotNull(modelValidatorFactory);
    Measure measure = Measure.builder().id("1").groups(List.of()).build();
    ModelValidator validator = modelValidatorFactory.getModelValidator(ModelType.QDM_5_6);
    assertTrue(validator instanceof QdmModelValidator);
    try {
      validator.validateGroups(measure);
      fail("Should fail because, there should be at least one population criteria");
    } catch (InvalidResourceStateException e) {
      assertEquals(
          "Response could not be completed for Measure with ID 1, since there is no population criteria on the measure.",
          e.getMessage());
    }
  }

  @Test
  void createQicoreModelValidatorTest() {
    assertNotNull(modelValidatorFactory);

    ModelValidator validator = modelValidatorFactory.getModelValidator(ModelType.QI_CORE);
    assertTrue(validator instanceof QiCoreModelValidator);
  }

  @Test
  void useQicoreModelValidatorTestNoStratifications() {
    assertNotNull(modelValidatorFactory);
    Group group = Group.builder().stratifications(new ArrayList<Stratification>()).build();
    ModelValidator validator = modelValidatorFactory.getModelValidator(ModelType.QI_CORE);
    assertTrue(validator instanceof QiCoreModelValidator);
    try {
      validator.validateGroupAssociations(group);

    } catch (Exception e) {
      fail(e);
    }
  }

  @Test
  void useQicoreModelValidatorTestHasInvalidAssociation() {
    assertNotNull(modelValidatorFactory);
    Stratification strat = new Stratification();
    List<Stratification> strats = new ArrayList<>();

    strats.add(strat);

    Group group = Group.builder().stratifications(strats).build();
    ModelValidator validator = modelValidatorFactory.getModelValidator(ModelType.QI_CORE);
    assertTrue(validator instanceof QiCoreModelValidator);
    try {
      validator.validateGroupAssociations(group);
      fail("Should fail because QICore strat association can't be null");
    } catch (InvalidGroupException e) {
      assertEquals(
          "QI-Core group stratifications should be associated to a valid population type.",
          e.getMessage());
    }
  }

  @Test
  void useQicoreModelValidatorTestHasAssociation() {
    assertNotNull(modelValidatorFactory);
    Stratification strat = new Stratification();
    List<Stratification> strats = new ArrayList<>();
    strat.setAssociation(PopulationType.INITIAL_POPULATION);
    strats.add(strat);

    Group group = Group.builder().stratifications(strats).build();
    ModelValidator validator = modelValidatorFactory.getModelValidator(ModelType.QI_CORE);
    assertTrue(validator instanceof QiCoreModelValidator);
    try {
      validator.validateGroupAssociations(group);

    } catch (Exception e) {
      fail(e);
    }
  }

  @Test
  void useQicoreModelValidatorTestMeasureHasNoGroup() {
    assertNotNull(modelValidatorFactory);
    Measure measure = Measure.builder().id("1").groups(List.of()).build();
    ModelValidator validator = modelValidatorFactory.getModelValidator(ModelType.QI_CORE);
    assertTrue(validator instanceof QiCoreModelValidator);
    try {
      validator.validateGroups(measure);
      fail("Should fail because, there should be at least one population criteria");
    } catch (InvalidResourceStateException e) {
      assertEquals(
          "Response could not be completed for Measure with ID 1, since there is no population criteria on the measure.",
          e.getMessage());
    }
  }

  @Test
  void useQicoreModelValidatorTestMeasureHasGroupWithNoTypes() {
    assertNotNull(modelValidatorFactory);
    Group group = Group.builder().build();
    Measure measure = Measure.builder().id("1").groups(List.of(group)).build();
    ModelValidator validator = modelValidatorFactory.getModelValidator(ModelType.QI_CORE);
    assertTrue(validator instanceof QiCoreModelValidator);
    try {
      validator.validateGroups(measure);
      fail("Should fail because QICore group should have types");
    } catch (InvalidResourceStateException e) {
      assertEquals(
          "Response could not be completed for Measure with ID 1, since there is at least one Population Criteria with no type.",
          e.getMessage());
    }
  }

  @Test
  void testValidateCqlErrorsWhenMeasureHasCQLErrors() {
    assertNotNull(modelValidatorFactory);
    Measure measure =
        Measure.builder()
            .id("1")
            .errors(List.of(MeasureErrorType.MISMATCH_CQL_POPULATION_RETURN_TYPES))
            .build();
    ModelValidator validator = modelValidatorFactory.getModelValidator(ModelType.QI_CORE);
    assertTrue(validator instanceof QiCoreModelValidator);
    assertThrows(InvalidResourceStateException.class, () -> validator.validateCqlErrors(measure));
  }

  @Test
  void testValidateCqlErrorsWhenIsCQLErrorIsTrue() {
    assertNotNull(modelValidatorFactory);
    Measure measure = Measure.builder().id("1").cqlErrors(true).build();
    ModelValidator validator = modelValidatorFactory.getModelValidator(ModelType.QDM_5_6);
    assertTrue(validator instanceof QdmModelValidator);
    assertThrows(InvalidResourceStateException.class, () -> validator.validateCqlErrors(measure));
  }

  @Test
  void testValidateCqlErrorsWhenNoErrors() {
    assertNotNull(modelValidatorFactory);
    Measure measure =
        Measure.builder().id("1").errors(Collections.emptyList()).cqlErrors(false).build();
    ModelValidator validator = modelValidatorFactory.getModelValidator(ModelType.QI_CORE);
    assertTrue(validator instanceof QiCoreModelValidator);
    assertDoesNotThrow(() -> validator.validateCqlErrors(measure));
  }
}
