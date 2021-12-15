package cms.gov.madie.measure.models;

import java.util.Date;

import javax.validation.GroupSequence;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class Measure {

  @Id private String id;

  private String measureHumanReadableId;
  private String measureSetId;
  private String version;
  private String revisionNumber;
  private String state;

  @NotBlank(
      groups = {ValidationOrder1.class},
      message = "Measure Name is Required")
  @Length(
      min = 1,
      max = 500,
      groups = {ValidationOrder2.class},
      message = "Measure Name contains at least one letter and can not be more than 500 characters")
  @Pattern(
      regexp = "^[^_]+$",
      groups = {
        ValidationOrder3.class,
      },
      message = "Measure Name can not contain underscores")
  private String measureName;

  private String cql;
  private Date createdAt;
  private String createdBy;
  private Date lastModifiedAt;
  private String lastModifiedBy;
  private String model;
  private MeasureMetaData measureMetaData = new MeasureMetaData();

  @GroupSequence({
    Measure.ValidationOrder1.class,
    Measure.ValidationOrder2.class,
    Measure.ValidationOrder3.class,
  })
  public interface ValidationSequence {}

  public interface ValidationOrder1 {}

  public interface ValidationOrder2 {}

  public interface ValidationOrder3 {}
}
