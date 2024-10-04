package cms.gov.madie.measure.repositories;

import gov.cms.madie.models.measure.TestCaseSequence;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TestCaseSequenceRepository extends MongoRepository<TestCaseSequence, String> {}
