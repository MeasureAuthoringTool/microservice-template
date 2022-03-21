package cms.gov.madie.measure.utils;

import cms.gov.madie.measure.models.MeasurePopulation;
import cms.gov.madie.measure.models.MeasurePopulationOption;
import cms.gov.madie.measure.models.MeasureScoring;
import org.springframework.data.util.Pair;

import java.util.List;
import java.util.Map;

public interface ScoringPopulationDefinition {
  Map<MeasureScoring, List<MeasurePopulationOption>> SCORING_POPULATION_MAP =
      Map.of(
          MeasureScoring.RATIO,
          List.of(
              new MeasurePopulationOption(MeasurePopulation.INITIAL_POPULATION, true),
              new MeasurePopulationOption(MeasurePopulation.NUMERATOR, true),
              new MeasurePopulationOption(MeasurePopulation.NUMERATOR_EXCLUSION, false),
              new MeasurePopulationOption(MeasurePopulation.DENOMINATOR, true),
              new MeasurePopulationOption(MeasurePopulation.DENOMINATOR_EXCLUSION, false)),
          MeasureScoring.PROPORTION,
          List.of(
              new MeasurePopulationOption(MeasurePopulation.INITIAL_POPULATION, true),
              new MeasurePopulationOption(MeasurePopulation.NUMERATOR, true),
              new MeasurePopulationOption(MeasurePopulation.NUMERATOR_EXCLUSION, false),
              new MeasurePopulationOption(MeasurePopulation.DENOMINATOR, true),
              new MeasurePopulationOption(MeasurePopulation.DENOMINATOR_EXCLUSION, false),
              new MeasurePopulationOption(MeasurePopulation.DENOMINATOR_EXCEPTION, false)),
          MeasureScoring.CONTINUOUS_VARIABLE,
          List.of(
              new MeasurePopulationOption(MeasurePopulation.INITIAL_POPULATION, true),
              new MeasurePopulationOption(MeasurePopulation.MEASURE_POPULATION, true),
              new MeasurePopulationOption(MeasurePopulation.MEASURE_POPULATION_EXCLUSION, false)
          ),
          MeasureScoring.COHORT,
          List.of(new MeasurePopulationOption(MeasurePopulation.INITIAL_POPULATION, true))
      );
}
