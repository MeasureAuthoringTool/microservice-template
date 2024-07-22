package cms.gov.madie.measure.repositories;

import gov.cms.madie.models.measure.Measure;

import java.util.List;

public interface MeasureCmsIdRepository {
  List<Measure> findAllByModelAndCmsId(String modelName, Integer qdmCmsId);
}
