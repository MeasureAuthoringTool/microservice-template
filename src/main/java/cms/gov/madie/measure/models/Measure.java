package cms.gov.madie.measure.models;

import java.util.Date;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.MongoId;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import cms.gov.madie.measure.serializer.ObjectIdSerializer;
import lombok.Data;

@Data
public class Measure {

  @MongoId
  @JsonSerialize(using = ObjectIdSerializer.class)
  ObjectId id;

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
