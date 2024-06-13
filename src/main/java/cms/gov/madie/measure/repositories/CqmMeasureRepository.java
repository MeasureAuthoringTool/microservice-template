package cms.gov.madie.measure.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import gov.cms.madie.models.cqm.CqmMeasure;

public interface CqmMeasureRepository extends MongoRepository<CqmMeasure, String> {
  @Query(value = "{'hqmf_set_id': ?0, 'hqmf_version_number': ?1 }")
  CqmMeasure findByHqmfSetIdAndHqmfVersionNumber(String id, String versionId);
}
