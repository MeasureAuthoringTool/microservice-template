package cms.gov.madie.measure.dto;

import lombok.*;

import java.util.List;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class MeasureTestCaseValidationReport {
  private String measureName;
  private String measureId;
  private String measureSetId;
  private String measureVersionId;

  @Singular private List<TestCaseValidationReport> testCaseValidationReports;
}
