package cms.gov.madie.measure.dto.qrda;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class PopulationDTO {
  String name;
  int expected;
  int actual;
  boolean pass;
}
