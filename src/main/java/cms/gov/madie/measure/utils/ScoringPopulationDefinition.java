package cms.gov.madie.measure.utils;

import cms.gov.madie.measure.models.MeasurePopulation;
import cms.gov.madie.measure.models.MeasureScoring;

import java.util.List;
import java.util.Map;

public interface ScoringPopulationDefinition {
  Map<MeasureScoring, List<MeasurePopulation>> SCORING_POPULATION_MAP =
      Map.of(
          MeasureScoring.RATIO,
          List.of(
              MeasurePopulation.INITIAL_POPULATION,
              MeasurePopulation.NUMERATOR,
              MeasurePopulation.NUMERATOR_EXCLUSION,
              MeasurePopulation.DENOMINATOR,
              MeasurePopulation.DENOMINATOR_EXCLUSION),
          MeasureScoring.PROPORTION,
          List.of(
              MeasurePopulation.INITIAL_POPULATION,
              MeasurePopulation.NUMERATOR,
              MeasurePopulation.NUMERATOR_EXCLUSION,
              MeasurePopulation.DENOMINATOR,
              MeasurePopulation.DENOMINATOR_EXCLUSION,
              MeasurePopulation.DENOMINATOR_EXCEPTION),
          MeasureScoring.CONTINUOUS_VARIABLE,
          List.of(
              MeasurePopulation.INITIAL_POPULATION,
              MeasurePopulation.MEASURE_POPULATION,
              MeasurePopulation.MEASURE_POPULATION_EXCLUSION),
          MeasureScoring.COHORT,
          List.of(MeasurePopulation.INITIAL_POPULATION));
}
