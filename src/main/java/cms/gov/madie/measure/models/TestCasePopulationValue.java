package cms.gov.madie.measure.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCasePopulationValue {

  private MeasurePopulation name;
  private Boolean expected;
  private Boolean actual;

}
