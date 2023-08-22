package cms.gov.madie.measure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ImpactedMeasureValidationReport {
        private String measureName;
        private String measureId;
        private String measureSetId;
        private String measureVersionId;
        private String measureOwner;
        private int impactedTestCasesCount;
}
