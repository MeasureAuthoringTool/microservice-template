package cms.gov.madie.measure.models;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;

@Data
@NoArgsConstructor
public class TestCase {
  private String id;
  private String name;
  private String series;
  private String description;
  private Date createdAt;
  private String createdBy;
  private Date lastModifiedAt;
  private String lastModifiedBy;
}
