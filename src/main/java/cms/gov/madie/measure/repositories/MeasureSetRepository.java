package cms.gov.madie.measure.repositories;

import gov.cms.madie.models.measure.MeasureSet;
import org.springframework.data.mongodb.repository.Query;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;;

public interface MeasureSetRepository extends MongoRepository<MeasureSet, String> {

  @Query("{measureSetId : ?0}")
  Optional<MeasureSet> findMeasureSetByMeasureSetId(String measureSetId);
}
