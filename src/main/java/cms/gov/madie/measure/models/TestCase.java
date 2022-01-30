package cms.gov.madie.measure.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCase {
  private String id;
  private String name;
  private String title;
  private String series;
  private String description;
  private Date createdAt;
  private String createdBy;
  private Date lastModifiedAt;
  private String lastModifiedBy;
  private String json;
}
