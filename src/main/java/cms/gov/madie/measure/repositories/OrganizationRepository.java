package cms.gov.madie.measure.repositories;

import gov.cms.madie.models.common.Organization;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrganizationRepository extends MongoRepository<Organization, String> {}
