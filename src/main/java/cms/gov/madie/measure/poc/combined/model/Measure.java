package cms.gov.madie.measure.poc.combined.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "modelType")
@JsonSubTypes({
  @Type(value = FhirMeasure.class, name = "QI-Core v4.1.1"),
  @Type(value = QDMMeasure.class, name = "QDM v5.6")
})
public abstract class Measure {

  @NotNull private String name;
}
