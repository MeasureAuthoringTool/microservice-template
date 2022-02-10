package cms.gov.madie.measure.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import cms.gov.madie.measure.models.Measure;
import org.springframework.data.mongodb.repository.Query;

public interface MeasureRepository extends MongoRepository<Measure, String> {
  Optional<Measure> findByCqlLibraryName(String cqlLibraryName);

  List<Measure> findAllByCreatedBy(String user);

  @Query(value = "{_id: ?0}", fields = "{'testCases.series': 1, _id: 0}")
  Optional<Measure> findAllTestCaseSeriesByMeasureId(String measureId);
}
