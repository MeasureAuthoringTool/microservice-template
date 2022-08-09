package cms.gov.madie.measure.repositories;

import gov.cms.madie.models.common.PopulationBasis;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PopulationBasisRepository extends MongoRepository<PopulationBasis, String> {}
