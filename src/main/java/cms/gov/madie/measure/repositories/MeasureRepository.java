package cms.gov.madie.measure.repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import gov.cms.madie.models.measure.Measure;

import org.springframework.data.mongodb.repository.Query;

public interface MeasureRepository extends MongoRepository<Measure, String> {
  @Query("{cqlLibraryName : ?0, active : true}")
  Optional<Measure> findByCqlLibraryName(String cqlLibraryName);

  Optional<Measure> findByIdAndActive(String id, Boolean active);

  Page<Measure> findAllByActive(Boolean active, Pageable page);

  @Query(
      "{$or: [{createdBy: { $regex : ?0, $options: 'i' }, active : ?1}, "
          + "{'acls.userId' : { $regex : ?0, $options: 'i' } , 'acls.roles' : ?2}]}")
  Page<Measure> findAllByCreatedByAndActiveOrShared(
      String user, Boolean active, String shared, Pageable page);

  @Query(value = "{_id: ?0}", fields = "{'testCases.series': 1, _id: 0}")
  Optional<Measure> findAllTestCaseSeriesByMeasureId(String measureId);

  @Query(value = "{'groups._id': ?0}")
  Optional<Measure> findGroupById(String groupId);

  @Query(
      " {$and: [{active : true} ,  "
          + "{$or: [{'measureName' : { $regex : ?0, $options: 'i' } },"
          + "{'ecqmTitle' : { $regex : ?0, $options: 'i' }}]} "
          + "]}")
  Page<Measure> findAllByMeasureNameOrEcqmTitle(String criteria, Pageable page);

  @Query(
      " {$and: [{'createdBy' :  { $regex : ?1, $options: 'i' }, active : true} ,  "
          + "{$or: [{'measureName' : { $regex : ?0, $options: 'i' } },"
          + "{'ecqmTitle' : { $regex : ?0, $options: 'i' }}]} "
          + "]}")
  Page<Measure> findAllByMeasureNameOrEcqmTitleForCurrentUser(
      String criteria, Pageable page, String user);
}
