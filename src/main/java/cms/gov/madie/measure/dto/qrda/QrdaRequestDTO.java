package cms.gov.madie.measure.dto.qrda;

import gov.cms.madie.models.measure.Measure;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class QrdaRequestDTO {
  Measure measure;
  Object options;
  QrdaGroupDTO[] groupDTOs;
}
