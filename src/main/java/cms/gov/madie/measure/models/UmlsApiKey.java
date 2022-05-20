package cms.gov.madie.measure.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Data
@AllArgsConstructor
@Builder(toBuilder = true)
@NoArgsConstructor
@Component
public class UmlsApiKey {
  private String apiKey;
  private String harpId;
}
