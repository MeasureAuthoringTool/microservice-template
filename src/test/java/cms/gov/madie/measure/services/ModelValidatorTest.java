package cms.gov.madie.measure.services;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cms.gov.madie.measure.exceptions.InvalidResourceStateException;
import cms.gov.madie.measure.factories.ModelValidatorFactory;
import gov.cms.madie.models.measure.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import cms.gov.madie.measure.exceptions.InvalidGroupException;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.common.Organization;

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
    strat.setCqlDefinition("Initial Population");
    strats.add(strat);

    Group group = Group.builder().stratifications(strats).build();
    ModelValidator validator = modelValidatorFactory.getModelValidator(ModelType.QDM_5_6);
    assertTrue(validator instanceof QdmModelValidator);
    try {
      validator.validateGroupAssociations(group);
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
      // Should no longer throw error
    } catch (InvalidGroupException e) {
      fail("Should not fail because QICore strat association can be null");
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
  void testQiCoreModelValidatorTestMeasureDoesHaveMeasuretypes() {
    assertNotNull(modelValidatorFactory);
    Measure measure =
        Measure.builder()
            .id("1")
            .groups(
                List.of(
                    Group.builder().measureGroupTypes(List.of(MeasureGroupTypes.OUTCOME)).build()))
            .build();
    ModelValidator validator = modelValidatorFactory.getModelValidator(ModelType.QI_CORE);
    assertTrue(validator instanceof QiCoreModelValidator);
    assertDoesNotThrow(() -> validator.validateGroups(measure));
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

  @Test
  void testQdmModelValidatorTestMeasureHasNoMeasuretypes() {
    assertNotNull(modelValidatorFactory);
    QdmMeasure measure =
        QdmMeasure.builder().id("1").groups(List.of(Group.builder().build())).build();
    ModelValidator validator = modelValidatorFactory.getModelValidator(ModelType.QDM_5_6);
    assertTrue(validator instanceof QdmModelValidator);
    try {
      validator.validateGroups(measure);
      fail("Should fail because, there should be at least one population criteria");
    } catch (InvalidResourceStateException e) {
      assertEquals(
          "Response could not be completed for Measure with ID 1, since there are no measure types for the measure.",
          e.getMessage());
    }
  }

  @Test
  void testQdmModelValidatorTestMeasureDoesHaveMeasuretypes() {
    assertNotNull(modelValidatorFactory);
    QdmMeasure measure =
        QdmMeasure.builder()
            .id("1")
            .groups(List.of(Group.builder().build()))
            .baseConfigurationTypes(List.of(BaseConfigurationTypes.OUTCOME))
            .build();
    ModelValidator validator = modelValidatorFactory.getModelValidator(ModelType.QDM_5_6);
    assertTrue(validator instanceof QdmModelValidator);
    assertDoesNotThrow(() -> validator.validateGroups(measure));
  }

  @Test
  void
      useQicoreModelValidatorTestDraftMeasureHasNoImprovementNotationInAtLeastOnePopulationCriteria() {
    assertNotNull(modelValidatorFactory);
    Group group1 =
        Group.builder()
            .measureGroupTypes(List.of(MeasureGroupTypes.OUTCOME))
            .improvementNotation("Decreased score indicates improvement")
            .build();
    Group group2 = Group.builder().measureGroupTypes(List.of(MeasureGroupTypes.OUTCOME)).build();

    Measure measure =
        Measure.builder()
            .id("1")
            .groups(List.of(group1, group2))
            .measureMetaData(MeasureMetaData.builder().draft(true).build())
            .build();

    ModelValidator validator = modelValidatorFactory.getModelValidator(ModelType.QI_CORE);
    assertTrue(validator instanceof QiCoreModelValidator);
    try {
      validator.validateGroups(measure);
      fail(
          "Should fail because, since there is at least one Population Criteria with no improvement notation");
    } catch (InvalidResourceStateException e) {
      assertEquals(
          "Response could not be completed for Measure with ID 1, since there is at least one Population Criteria with no improvement notation.",
          e.getMessage());
    }
  }

  @Test
  void
      useQicoreModelValidatorTestVersionedMeasureHasNoImprovementNotationInAtLeastOnePopulationCriteria() {
    assertNotNull(modelValidatorFactory);
    Group group1 =
        Group.builder()
            .measureGroupTypes(List.of(MeasureGroupTypes.OUTCOME))
            .improvementNotation("Decreased score indicates improvement")
            .build();
    Group group2 = Group.builder().measureGroupTypes(List.of(MeasureGroupTypes.OUTCOME)).build();

    Measure measure =
        Measure.builder()
            .id("1")
            .groups(List.of(group1, group2))
            .measureMetaData(MeasureMetaData.builder().draft(false).build())
            .build();

    ModelValidator validator = modelValidatorFactory.getModelValidator(ModelType.QI_CORE);
    assertTrue(validator instanceof QiCoreModelValidator);
    assertDoesNotThrow(() -> validator.validateGroups(measure));
  }

  @Test
  public void testValidateMetadataNoMetadata() {
    Measure measure = Measure.builder().build();
    ModelValidator validator = modelValidatorFactory.getModelValidator(ModelType.QI_CORE);
    //    measureUtil.validateMetadata(measure);
    assertDoesNotThrow(() -> validator.validateMetadata(measure));
  }

  @Test
  public void testValidateMetadataNoDevelopers() {
    Measure measure =
        Measure.builder()
            .id("measureId")
            .measureMetaData(MeasureMetaData.builder().build())
            .build();
    ModelValidator validator = modelValidatorFactory.getModelValidator(ModelType.QI_CORE);

    Exception ex =
        assertThrows(
            InvalidResourceStateException.class, () -> validator.validateMetadata(measure));
    assertThat(
        ex.getMessage(),
        is(
            equalTo(
                "Response could not be completed for Measure with ID measureId, since there are no associated developers in metadata.")));
  }

  @Test
  public void testValidateMetadataNoSteward() {
    Measure measure =
        Measure.builder()
            .id("measureId")
            .measureMetaData(
                MeasureMetaData.builder()
                    .developers(List.of(Organization.builder().id("OrgId").build()))
                    .build())
            .build();
    ModelValidator validator = modelValidatorFactory.getModelValidator(ModelType.QI_CORE);
    Exception ex =
        assertThrows(
            InvalidResourceStateException.class, () -> validator.validateMetadata(measure));
    assertThat(
        ex.getMessage(),
        is(
            equalTo(
                "Response could not be completed for Measure with ID measureId, since there is no associated steward in metadata.")));
  }

  @Test
  public void testValidateMetadataNoDescription() {
    Measure measure =
        Measure.builder()
            .id("measureId")
            .measureMetaData(
                MeasureMetaData.builder()
                    .developers(List.of(Organization.builder().id("OrgId").build()))
                    .steward(Organization.builder().id("OrgId").build())
                    .build())
            .build();
    ModelValidator validator = modelValidatorFactory.getModelValidator(ModelType.QI_CORE);
    Exception ex =
        assertThrows(
            InvalidResourceStateException.class, () -> validator.validateMetadata(measure));
    assertThat(
        ex.getMessage(),
        is(
            equalTo(
                "Response could not be completed for Measure with ID measureId, since there is no description in metadata.")));
  }

  @Test
  public void testValidateMetadataValid() {
    Measure measure =
        Measure.builder()
            .id("measureId")
            .measureMetaData(
                MeasureMetaData.builder()
                    .developers(List.of(Organization.builder().id("OrgId").build()))
                    .steward(Organization.builder().id("OrgId").build())
                    .description("test description")
                    .build())
            .build();
    ModelValidator validator = modelValidatorFactory.getModelValidator(ModelType.QI_CORE);
    assertDoesNotThrow(() -> validator.validateMetadata(measure));
  }
}
