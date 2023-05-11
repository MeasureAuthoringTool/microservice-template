package cms.gov.madie.measure.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import gov.cms.madie.models.measure.Measure;

public interface MeasureRepository
    extends MongoRepository<Measure, String>, MeasureVersionRepository {
  @Query("{cqlLibraryName : ?0, active : true}")
  Optional<Measure> findByCqlLibraryName(String cqlLibraryName);

  Optional<Measure> findByIdAndActive(String id, Boolean active);

  Page<Measure> findAllByActive(Boolean active, Pageable page);

  @Query(
      collation = "{ 'locale': 'en_US', 'strength': 2}",
      value =
          "{$or: [{createdBy: ?0 , active : ?1}, " + "{'acls.userId' : ?0 , 'acls.roles' : ?2}]}")
  Page<Measure> findAllByCreatedByAndActiveOrShared(
      String user, Boolean active, String shared, Pageable page);

  @Aggregation(
      pipeline = {
        "{'$sort': {'createdAt':1}}",
        "{'$group': {'_id': '$_id',"
            + "'measureSetId': {'$first':'$measureSetId'},"
            + "'createdBy': {'$first':'$createdBy'}}}"
      })
  List<Measure> findOldestMeasureSet();

  @Query(value = "{_id: ?0}", fields = "{'testCases.series': 1, _id: 0}")
  Optional<Measure> findAllTestCaseSeriesByMeasureId(String measureId);

  @Query(value = "{'groups._id': ?0}")
  Optional<Measure> findGroupById(String groupId);

  @Query(
      " {$and: [{active : true} ,  "
          + "{$or: [{'measureName' : { $regex : /\\Q?0\\E/, $options: 'i' } },"
          + "{'ecqmTitle' : { $regex : /\\Q?0\\E/, $options: 'i' }}]} "
          + "]}")
  Page<Measure> findAllByMeasureNameOrEcqmTitle(String criteria, Pageable page);

  @Query(
      collation = "{ 'locale': 'en_US', 'strength': 2}",
      value =
          " {$and: [{$or:[{'createdBy' : ?1, active : true},"
              + "{'acls.userId' : ?1 , 'acls.roles' : 'SHARED_WITH'}]},"
              + "{$or: [{'measureName' : { $regex : /\\Q?0\\E/, $options: 'i' } },"
              + "{'ecqmTitle' : { $regex : /\\Q?0\\E/, $options: 'i' }}]} "
              + "]}")
  Page<Measure> findAllByMeasureNameOrEcqmTitleForCurrentUser(
      String criteria, Pageable page, String user);

  boolean existsByMeasureSetIdAndActiveAndMeasureMetaDataDraft(
      String setId, boolean active, boolean draft);
  // Map measureSetId, boolean (ie.,
  // id 1 - drafted , 2 - versioned  setId 4 4, false
  // id 1 - versioned , 2 - versioned setId 4 4, true
  List<Measure> findAllByMeasureSetIdInAndActiveAndMeasureMetaDataDraft(
      List<String> setIds, boolean active, boolean draft);
}
