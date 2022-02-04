package cms.gov.madie.measure.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

import javax.validation.GroupSequence;

import org.hibernate.validator.constraints.Length;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCase {
  private String id;
  private String name;
  private String title;
  private String series;

  @Length(
      max = 250,
      groups = {ValidationOrder1.class},
      message = "Test Case Description can not be more than 250 characters.")
  private String description;

  private Date createdAt;
  private String createdBy;
  private Date lastModifiedAt;
  private String lastModifiedBy;
  private String json;

  @GroupSequence({
    TestCase.ValidationOrder1.class,
  })
  public interface ValidationSequence {}

  public interface ValidationOrder1 {}
}
