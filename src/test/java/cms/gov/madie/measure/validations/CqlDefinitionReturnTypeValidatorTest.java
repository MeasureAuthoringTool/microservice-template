package cms.gov.madie.measure.validations;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.core.JsonProcessingException;
import cms.gov.madie.measure.utils.ResourceUtil;
import gov.cms.madie.models.measure.DefDescPair;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.PopulationType;
import gov.cms.madie.models.measure.SupplementalData;

class CqlDefinitionReturnTypeValidatorTest implements ResourceUtil {

  @Test
  void testValidateCqlDefinitionReturnTypes() throws JsonProcessingException {
    // new group, not in DB, so no ID
    Group group1 =
        Group.builder()
            .scoring("Cohort")
            .populationBasis("Encounter")
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

    CqlDefinitionReturnTypeValidator validator = new CqlDefinitionReturnTypeValidator();
    validator.validateCqlDefinitionReturnTypes(group1, elmJson);
  }

  @Test
  void testValidateSdeDefinitions_Valid() {
    String elmJson = getData("/test_elm.json");

    CqlDefinitionReturnTypeValidator validator = new CqlDefinitionReturnTypeValidator();
    DefDescPair sde =
        SupplementalData.builder().definition("fun23").description("Please Help Me").build();
    boolean isValid = validator.isDefineInElm(sde, elmJson);
    assertThat(isValid, is(true));
  }

  @Test
  void testValidateSdeDefinitions_Invalid() {
    String elmJson = getData("/test_elm.json");

    CqlDefinitionReturnTypeValidator validator = new CqlDefinitionReturnTypeValidator();
    DefDescPair sde =
        SupplementalData.builder().definition("fun34").description("Please Help Me").build();
    boolean isValid = validator.isDefineInElm(sde, elmJson);
    assertThat(isValid, is(false));
  }

  @Test
  void testValidateSdeDefinitions_InvalidJson() {
    String elmJson = "{ curroped: json";

    CqlDefinitionReturnTypeValidator validator = new CqlDefinitionReturnTypeValidator();
    DefDescPair sde =
        SupplementalData.builder().definition("fun34").description("Please Help Me").build();
    boolean isValid = validator.isDefineInElm(sde, elmJson);
    assertThat(isValid, is(false));
  }
}
