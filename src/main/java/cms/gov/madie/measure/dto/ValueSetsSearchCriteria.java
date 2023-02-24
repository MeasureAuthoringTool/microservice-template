package cms.gov.madie.measure.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder(toBuilder = true)
public class ValueSetsSearchCriteria {

  private String profile;
  private boolean includeDraft;
  private List<ValueSetParams> valueSetParams;

  @Data
  @Builder(toBuilder = true)
  public static class ValueSetParams {
    private String oid;
    private String release;
    private String version;
  }
}
