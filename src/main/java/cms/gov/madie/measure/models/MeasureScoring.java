package cms.gov.madie.measure.models;

import com.fasterxml.jackson.annotation.JsonProperty;

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
}
