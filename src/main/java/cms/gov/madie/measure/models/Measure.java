package cms.gov.madie.measure.models;

import java.util.Date;
import org.springframework.data.annotation.Id;
import lombok.Data;

@Data
public class Measure {

  @Id
  private String id;
  private String measureHumanReadableId;
  private String measureSetId;
  private String version;
  private String revisionNumber;
  private String state;
  private String measureName;
  private String cql;
  private Date createdAt;
  private String createdBy;
  private Date lastModifiedAt;
  private String lastModifiedBy;
  private String model;
}
