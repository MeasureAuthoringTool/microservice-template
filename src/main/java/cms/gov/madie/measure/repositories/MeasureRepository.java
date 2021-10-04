package cms.gov.madie.measure.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import cms.gov.madie.measure.models.Measure;

public interface MeasureRepository extends MongoRepository<Measure, String> {}
