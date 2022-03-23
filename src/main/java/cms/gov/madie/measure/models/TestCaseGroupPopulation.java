package cms.gov.madie.measure.models;

import cms.gov.madie.measure.validations.ValidScoringPopulation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidScoringPopulation
public class TestCaseGroupPopulation {
  private String group;

  @EnumValidator(
      enumClass = MeasureScoring.class,
      message = "Scoring must be a valid MADiE scoring type")
  private String scoring;

  private List<TestCasePopulationValue> populationValues;
}
