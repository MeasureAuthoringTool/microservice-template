package cms.gov.madie.measure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class TestCaseValidationReport {
  private String testCaseId;
  private String patientId;
  private boolean previousValidResource;
  private boolean currentValidResource;
}
