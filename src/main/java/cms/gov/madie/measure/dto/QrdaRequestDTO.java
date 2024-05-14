package cms.gov.madie.measure.dto;

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
  String coveragePercentage;
  Integer passPercentage;
  String passFailRatio;
  Object options;
  Object[] testCaseDtos;
}
