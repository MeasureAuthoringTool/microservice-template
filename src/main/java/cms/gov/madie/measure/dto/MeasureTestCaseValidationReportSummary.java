package cms.gov.madie.measure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class MeasureTestCaseValidationReportSummary {
    List<MeasureTestCaseValidationReport> reports;
    List<ImpactedMeasureValidationReport> impactedMeasures;
}

