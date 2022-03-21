package cms.gov.madie.measure.models;

import java.util.Map;

public interface GroupScoringPopulation {
  String getScoring();
  Map<MeasurePopulation, ?> getPopulation();
}
