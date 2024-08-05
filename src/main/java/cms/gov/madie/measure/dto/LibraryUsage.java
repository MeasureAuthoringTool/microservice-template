package cms.gov.madie.measure.dto;

import gov.cms.madie.models.common.Version;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LibraryUsage {
  private String name;
  // TODO: getting version in string would be beneficial
  private Version version;
  private String owner;
}
