package cms.gov.madie.measure.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

public enum MeasureScoring {
  @JsonProperty("Cohort")
  COHORT("Cohort"),
  @JsonProperty("Continuous Variable")
  CONTINUOUS_VARIABLE("Continuous Variable"),
  @JsonProperty("Proportion")
  PROPORTION("Proportion"),
  @JsonProperty("Ratio")
  RATIO("Ratio");

  private final String text;

  MeasureScoring(String text) {
    this.text = text;
  }

  @Override
  public String toString() {
    return this.text;
  }

  public static MeasureScoring valueOfText(String text) {
    return Arrays.stream(MeasureScoring.values())
        .filter(s -> s.text.equals(text))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No enum constant " + MeasureScoring.class.getCanonicalName() + "." + text));
  }
}
