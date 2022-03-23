package cms.gov.madie.measure.models;

import cms.gov.madie.measure.validations.ValidGroupScoringPopulation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import javax.validation.constraints.NotNull;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidGroupScoringPopulation
public class Group implements GroupScoringPopulation {
  @Id private String id;

  @NotNull(
      message = "Scoring is required.",
      groups = {Measure.ValidationOrder5.class})
  @EnumValidator(
      enumClass = MeasureScoring.class,
      message = "Scoring must be a valid MADiE scoring type",
      groups = {Measure.ValidationOrder5.class})
  private String scoring;

  private Map<MeasurePopulation, String> population;
}
