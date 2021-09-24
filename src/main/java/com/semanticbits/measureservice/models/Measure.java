package com.semanticbits.measureservice.models;

import java.util.Date;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.MongoId;

import lombok.Data;

@Data
public class Measure {

  @MongoId ObjectId id;
  private String measureHumanReadableId;
  private String measureSetId;
  private String version;
  private String revisionNumber;
  private boolean state;
  private String name;
  private String cql;
  private Date createdAt;
  private String createdBy;
  private Date lastModifiedAt;
  private String lastModifiedBy;
}
