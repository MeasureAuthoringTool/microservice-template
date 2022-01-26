package cms.gov.madie.measure.models;

import java.time.Instant;

import javax.validation.GroupSequence;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Measure {

  @Id private String id;

  private String measureHumanReadableId;
  private String measureSetId;
  private String version;
  private String revisionNumber;
  private String state;

  @Indexed(unique = true)
  @NotBlank(
      groups = {ValidationOrder1.class},
      message = "Measure Library Name is required.")
  @Pattern(
      regexp = "^[A-Z][a-zA-Z0-9]*$",
      groups = {
        ValidationOrder2.class,
      },
      message = "Measure Library Name is invalid.")
  private String cqlLibraryName;

  @NotBlank(
      groups = {ValidationOrder1.class},
      message = "Measure Name is required.")
  @Length(
      min = 1,
      max = 500,
      groups = {ValidationOrder2.class},
      message = "Measure Name can not be more than 500 characters.")
  @Pattern(
      regexp = "^[^_]+$",
      groups = {ValidationOrder3.class},
      message = "Measure Name can not contain underscores.")
  @Pattern(
      regexp = ".*[a-zA-Z]+.*",
      groups = {ValidationOrder4.class},
      message = "A measure name must contain at least one letter.")
  private String measureName;

  private String cql;
  private List<TestCase> testCases;
  private Instant createdAt;
  private String createdBy;
  private Instant lastModifiedAt;
  private String lastModifiedBy;

  @EnumValidator(
      enumClass = ModelType.class,
      message = "MADiE was unable to complete your request, please try again.",
      groups = {ValidationOrder5.class})
  private String model;

  @NotBlank(
      groups = {ValidationOrder5.class},
      message = "Measure Scoring is required.")
  @EnumValidator(
      enumClass = MeasureScoring.class,
      groups = {ValidationOrder5.class})
  private String measureScoring;

  private MeasureMetaData measureMetaData = new MeasureMetaData();

  @GroupSequence({
    Measure.ValidationOrder1.class,
    Measure.ValidationOrder2.class,
    Measure.ValidationOrder3.class,
    Measure.ValidationOrder4.class,
    Measure.ValidationOrder5.class,
  })
  public interface ValidationSequence {}

  public interface ValidationOrder1 {}

  public interface ValidationOrder2 {}

  public interface ValidationOrder3 {}

  public interface ValidationOrder4 {}

  public interface ValidationOrder5 {}
}
