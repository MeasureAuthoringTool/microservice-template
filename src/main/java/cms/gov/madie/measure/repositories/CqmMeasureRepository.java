package cms.gov.madie.measure.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import gov.cms.madie.models.cqm.CqmMeasure;

public interface CqmMeasureRepository extends MongoRepository<CqmMeasure, String> {}
