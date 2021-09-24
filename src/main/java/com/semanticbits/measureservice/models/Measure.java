package com.semanticbits.measureservice.models;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.MongoId;

import lombok.Data;

@Data
public class Measure {

  @MongoId ObjectId id;
  private String measureHumanReadableId;
  private String measureSetId;
}
