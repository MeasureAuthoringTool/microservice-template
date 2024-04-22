package cms.gov.madie.measure.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class PackageDto {
  private byte[] exportPackage;
  private String translatorVersion;
  private boolean fromStorage;
}
