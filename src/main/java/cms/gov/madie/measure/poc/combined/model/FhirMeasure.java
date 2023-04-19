package cms.gov.madie.measure.poc.combined.model;

import java.util.List;
import javax.validation.Valid;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
public class FhirMeasure extends Measure {

  @Valid private List<Group> groups;
}
