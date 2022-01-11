package cms.gov.madie.measure.models;

public enum MeasureScoring {
  COHORT("Cohort"),
  CONTINUOUS_VARIABLE("Continuous Variable"),
  PROPORTION("Proportion"),
  RATIO("Ratio");

  private final String text;

  MeasureScoring(String text) {
    this.text = text;
  }

  public String getText() {
    return text;
  }
}
