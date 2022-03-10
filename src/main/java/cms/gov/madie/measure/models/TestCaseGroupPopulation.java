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
  private MeasureScoring scoring;
  private List<TestCasePopulationValue> populationValues;
}
