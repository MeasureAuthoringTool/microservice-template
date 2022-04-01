package cms.gov.madie.measure.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum MeasurePopulation {
  @JsonProperty("initialPopulation")
  INITIAL_POPULATION,
  @JsonProperty("numerator")
  NUMERATOR,
  @JsonProperty("numeratorExclusion")
  NUMERATOR_EXCLUSION,
  @JsonProperty("denominator")
  DENOMINATOR,
  @JsonProperty("denominatorExclusion")
  DENOMINATOR_EXCLUSION,
  @JsonProperty("denominatorException")
  DENOMINATOR_EXCEPTION,
  @JsonProperty("measurePopulation")
  MEASURE_POPULATION,
  @JsonProperty("measurePopulationExclusion")
  MEASURE_POPULATION_EXCLUSION,
  @JsonProperty("measureObservation")
  MEASURE_OBSERVATION;
}
