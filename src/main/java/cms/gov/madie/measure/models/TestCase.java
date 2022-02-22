package cms.gov.madie.measure.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;

import java.time.Instant;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class TestCase {
  private String id;
  private String name;
  private String title;
  private String series;
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
}
