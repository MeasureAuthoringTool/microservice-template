package cms.gov.madie.measure.poc.dto;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
public class MeasureDTO {

  private String name;

  private String modelType;
  private String scoring;

  private List<GroupDTO> groups;
}
