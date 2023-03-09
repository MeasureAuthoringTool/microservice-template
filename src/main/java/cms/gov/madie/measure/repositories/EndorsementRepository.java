package cms.gov.madie.measure.repositories;

import gov.cms.madie.models.common.EndorserOrganization;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EndorsementRepository extends MongoRepository<EndorserOrganization, String> {}
