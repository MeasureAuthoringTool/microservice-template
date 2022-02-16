package cms.gov.madie.measure.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hl7.fhir.r4.model.OperationOutcome;

import java.util.List;

@Data
public class HapiOperationOutcome {
  private int code;
  private String message;
  private String outcome;
  private List<OutcomeIssue> issues;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class OutcomeIssue {
    private String severity;
    private String code;
    private String diagnostics;
    private List<String> location;
  }
}


/*
,
    "profile": [ "http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient" ]
 */
