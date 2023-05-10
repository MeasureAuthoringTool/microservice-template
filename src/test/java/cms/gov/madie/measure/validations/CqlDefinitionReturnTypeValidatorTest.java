package cms.gov.madie.measure.validations;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.core.JsonProcessingException;

import cms.gov.madie.measure.exceptions.InvalidFhirGroupException;
import cms.gov.madie.measure.exceptions.InvalidReturnTypeForQdmException;
import cms.gov.madie.measure.utils.ResourceUtil;
import gov.cms.madie.models.measure.DefDescPair;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.MeasureGroupTypes;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.PopulationType;
import gov.cms.madie.models.measure.SupplementalData;

class CqlDefinitionReturnTypeValidatorTest implements ResourceUtil {

  CqlDefinitionReturnTypeValidator validator = new CqlDefinitionReturnTypeValidator();

  @Test
  void testValidateCqlDefinitionReturnTypes() throws JsonProcessingException {
    // new group, not in DB, so no ID
    Group group1 =
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

    String elmJson = getData("/test_elm.json");

    validator.validateCqlDefinitionReturnTypes(group1, elmJson);
  }

  @Test
  void testValidateSdeDefinitions_Valid() {
    String elmJson = getData("/test_elm.json");

    DefDescPair sde =
        SupplementalData.builder().definition("fun23").description("Please Help Me").build();
    boolean isValid = validator.isDefineInElm(sde, elmJson);
    assertThat(isValid, is(true));
  }

  @Test
  void testValidateSdeDefinitions_Invalid() {
    String elmJson = getData("/test_elm.json");

    DefDescPair sde =
        SupplementalData.builder().definition("fun34").description("Please Help Me").build();
    boolean isValid = validator.isDefineInElm(sde, elmJson);
    assertThat(isValid, is(false));
  }

  @Test
  void testValidateSdeDefinitions_InvalidJson() {
    String elmJson = "{ curroped: json";

    DefDescPair sde =
        SupplementalData.builder().definition("fun34").description("Please Help Me").build();
    boolean isValid = validator.isDefineInElm(sde, elmJson);
    assertThat(isValid, is(false));
  }

  @Test
  void testValidateCqlDefinitionReturnTypesInvalidGroup() {
    Group group1 =
        Group.builder()
            .scoring("Cohort")
            .populationBasis("boolean")
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

    String elmJson = getData("/test_elm.json");
    assertThrows(
        InvalidFhirGroupException.class,
        () -> validator.validateCqlDefinitionReturnTypes(group1, elmJson),
        "Measure Group Types and Population Basis are required for FHIR Measure Group.");

    group1.setPopulationBasis(null);
    group1.setMeasureGroupTypes(Arrays.asList(MeasureGroupTypes.OUTCOME));
    assertThrows(
        InvalidFhirGroupException.class,
        () -> validator.validateCqlDefinitionReturnTypes(group1, elmJson),
        "Measure Group Types and Population Basis are required for FHIR Measure Group.");
  }

  @Test
  void testValidateCqlDefinitionReturnTypesForQdmNullElm() {
    Group group1 =
        Group.builder()
            .scoring("Cohort")
            .measureGroupTypes(Arrays.asList(MeasureGroupTypes.OUTCOME))
            .populations(
                List.of(
                    new Population(
                        "id-1",
                        PopulationType.INITIAL_POPULATION,
                        "Initial Population",
                        null,
                        null),
                    new Population(
                        "id-2", PopulationType.INITIAL_POPULATION, "Denominator", null, null)))
            .groupDescription("Description")
            .scoringUnit("test-scoring-unit")
            .build();
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.validateCqlDefinitionReturnTypesForQdm(group1, null, true),
        "No definitions found.");
  }

  @Test
  void testValidateCqlDefinitionReturnTypesForQdmPatientBasedThrowsException()
      throws JsonProcessingException {
    Group group1 =
        Group.builder()
            .scoring("Cohort")
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

    String elmJson = getData("/test_elm.json");

    assertThrows(
        InvalidReturnTypeForQdmException.class,
        () -> validator.validateCqlDefinitionReturnTypesForQdm(group1, elmJson, true),
        "For Patient-based Measures, selected definitions must return a Boolean.");
  }

  @Test
  void testValidateCqlDefinitionReturnTypesForQdmPatientBasedSuccess()
      throws JsonProcessingException {
    Group group1 =
        Group.builder()
            .scoring("Cohort")
            .populations(
                List.of(
                    new Population(
                        "id-1", PopulationType.INITIAL_POPULATION, "boolIpp", null, null)))
            .groupDescription("Description")
            .scoringUnit("test-scoring-unit")
            .build();

    String elmJson = getData("/test_elm_with_boolean.json");

    validator.validateCqlDefinitionReturnTypesForQdm(group1, elmJson, true);
  }

  @Test
  void testValidateCqlDefinitionReturnTypesForQdmNonPatientBasedThrowsException()
      throws JsonProcessingException {
    Group group1 =
        Group.builder()
            .scoring("Cohort")
            .populations(
                List.of(
                    new Population(
                        "id-1",
                        PopulationType.INITIAL_POPULATION,
                        "Initial Population",
                        null,
                        null),
                    new Population(
                        "id-2", PopulationType.INITIAL_POPULATION, "SDE Payer", null, null)))
            .groupDescription("Description")
            .scoringUnit("test-scoring-unit")
            .build();

    String elmJson = getData("/test_elm.json");

    assertThrows(
        InvalidReturnTypeForQdmException.class,
        () -> validator.validateCqlDefinitionReturnTypesForQdm(group1, elmJson, false),
        "For Episode-based Measures, selected definitions must return a list of the same type.");
  }

  @Test
  void testValidateCqlDefinitionReturnTypesForNonQdmPatientBasedSuccess()
      throws JsonProcessingException {
    Group group1 =
        Group.builder()
            .scoring("Cohort")
            .populations(
                List.of(
                    new Population(
                        "id-1",
                        PopulationType.INITIAL_POPULATION,
                        "Initial Population",
                        null,
                        null),
                    new Population(
                        "id-2", PopulationType.INITIAL_POPULATION, "Denominator", null, null)))
            .groupDescription("Description")
            .scoringUnit("test-scoring-unit")
            .build();

    String elmJson = getData("/test_elm.json");

    validator.validateCqlDefinitionReturnTypesForQdm(group1, elmJson, false);
  }

  @Test
  void testValidateCqlDefinitionReturnTypesForNonQdmPatientBasedSuccessNoPopulations()
      throws JsonProcessingException {
    Group group1 =
        Group.builder()
            .scoring("Cohort")
            .groupDescription("Description")
            .scoringUnit("test-scoring-unit")
            .build();

    String elmJson = getData("/test_elm.json");

    validator.validateCqlDefinitionReturnTypesForQdm(group1, elmJson, false);
  }

  @Test
  void testValidateCqlDefinitionReturnTypesForNonQdmPatientBasedSuccessNoDefinition()
      throws JsonProcessingException {
    Group group1 =
        Group.builder()
            .scoring("Cohort")
            .groupDescription("Description")
            .scoringUnit("test-scoring-unit")
            .populations(
                List.of(
                    new Population("id-1", PopulationType.INITIAL_POPULATION, null, null, null)))
            .build();

    String elmJson = getData("/test_elm.json");

    validator.validateCqlDefinitionReturnTypesForQdm(group1, elmJson, false);
  }

  @Test
  void testValidateCqlDefinitionReturnTypesForQdmNonPatientBasedWithBoolean()
      throws JsonProcessingException {
    Group group1 =
        Group.builder()
            .scoring("Cohort")
            .populations(
                List.of(
                    new Population(
                        "id-1", PopulationType.INITIAL_POPULATION, "boolIpp", null, null)))
            .groupDescription("Description")
            .scoringUnit("test-scoring-unit")
            .build();

    String elmJson = getData("/test_elm_with_boolean.json");

    assertThrows(
        InvalidReturnTypeForQdmException.class,
        () -> validator.validateCqlDefinitionReturnTypesForQdm(group1, elmJson, false),
        "The selected definition does not align with the Episode-based Measure.");
  }
}
