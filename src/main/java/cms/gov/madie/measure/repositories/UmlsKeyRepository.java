package cms.gov.madie.measure.repositories;

import cms.gov.madie.measure.models.UmlsApiKey;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UmlsKeyRepository extends MongoRepository<UmlsApiKey, String> {
  Optional<UmlsApiKey> findByHarpId(String harpId);
}
