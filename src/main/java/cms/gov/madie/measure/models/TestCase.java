package cms.gov.madie.measure.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;

import java.time.Instant;

import javax.validation.GroupSequence;

import org.hibernate.validator.constraints.Length;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class TestCase {
  private String id;
  private String name;

  @Length(
      max = 250,
      groups = {ValidationOrder1.class},
      message = "Test Case Title can not be more than 250 characters.")
  private String title;

  @Length(
      max = 250,
      groups = {ValidationOrder1.class},
      message = "Test Case Series can not be more than 250 characters.")
  private String series;

  @Length(
      max = 250,
      groups = {ValidationOrder1.class},
      message = "Test Case Description can not be more than 250 characters.")
  private String description;

  private Instant createdAt;
  private String createdBy;
  private Instant lastModifiedAt;
  private String lastModifiedBy;
  @JsonIgnore
  private String resourceUri;
  @JsonIgnore
  private boolean isValidResource;
  private String json;

  @Transient
  private HapiOperationOutcome hapiOperationOutcome;

  @GroupSequence({
    TestCase.ValidationOrder1.class,
  })
  public interface ValidationSequence {}

  public interface ValidationOrder1 {}
}
