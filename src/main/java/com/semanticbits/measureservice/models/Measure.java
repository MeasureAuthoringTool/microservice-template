package com.semanticbits.measureservice.models;

import org.springframework.data.annotation.Id;

import lombok.Data;

@Data
public class Measure {

  @Id private Long id;
  private String measureHumanReadableId;
  private String measureSetId;
}
