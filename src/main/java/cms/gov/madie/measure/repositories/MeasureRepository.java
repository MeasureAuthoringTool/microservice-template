package cms.gov.madie.measure.repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import cms.gov.madie.measure.models.Measure;
import org.springframework.data.mongodb.repository.Query;

public interface MeasureRepository extends MongoRepository<Measure, String> {
  Optional<Measure> findByCqlLibraryName(String cqlLibraryName);

  Page<Measure> findAllByCreatedBy(String user, Pageable page);

  @Query(value = "{_id: ?0}", fields = "{'testCases.series': 1, _id: 0}")
  Optional<Measure> findAllTestCaseSeriesByMeasureId(String measureId);
}
