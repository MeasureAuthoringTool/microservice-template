package cms.gov.madie.measure.poc.dto.model;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
public class FhirMeasure implements Measure {

  @NotNull private String name;

  @Valid private List<Group> groups;
}
