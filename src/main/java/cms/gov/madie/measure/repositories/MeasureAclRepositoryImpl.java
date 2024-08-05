package cms.gov.madie.measure.repositories;

import cms.gov.madie.measure.dto.FacetDTO;
import cms.gov.madie.measure.dto.LibraryUsage;
import cms.gov.madie.measure.dto.MeasureListDTO;
import gov.cms.madie.models.access.RoleEnum;
import gov.cms.madie.models.library.CqlLibrary;
import gov.cms.madie.models.measure.Measure;

import org.apache.commons.lang3.StringUtils;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
public class MeasureAclRepositoryImpl implements MeasureAclRepository {
  private final MongoTemplate mongoTemplate;

  public MeasureAclRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  private LookupOperation getLookupOperation() {
    return LookupOperation.newLookup()
      .from("measureSet")
      .localField("measureSetId")
      .foreignField("measureSetId")
      .as("measureSet");
  }

  @Override
  public Page<MeasureListDTO> findMyActiveMeasures(
      String userId, Pageable pageable, String searchTerm) {
    // join measure and measure_set to lookup owner and ACL info
    LookupOperation lookupOperation = getLookupOperation();

    // prepare measure search criteria
    Criteria measureCriteria = Criteria.where("active").is(true);
    if (StringUtils.isNotBlank(searchTerm)) {
      measureCriteria.andOperator(
          new Criteria()
              .orOperator(
                  Criteria.where("measureName").regex(searchTerm, "i"),
                  Criteria.where("ecqmTitle").regex(searchTerm, "i")));
    }

    // prepare measure set search criteria(user is either owner or shared with)
    Criteria measureSetCriteria =
        new Criteria()
            .orOperator(
                Criteria.where("measureSet.owner").regex("^\\Q" + userId + "\\E$", "i"),
                Criteria.where("measureSet.acls.userId")
                    .regex("^\\Q" + userId + "\\E$", "i")
                    .and("measureSet.acls.roles")
                    .in(RoleEnum.SHARED_WITH));

    // combine measure and measure set criteria
    MatchOperation matchOperation =
        match(new Criteria().andOperator(measureCriteria, measureSetCriteria));

    FacetOperation facets =
        facet(sortByCount("id"))
            .as("count")
            .and(
                sort(pageable.getSort()),
                skip(pageable.getOffset()),
                limit(pageable.getPageSize()),
                project(MeasureListDTO.class))
            .as("queryResults");

    Aggregation pipeline = newAggregation(lookupOperation, matchOperation, facets);

    List<FacetDTO> results =
        mongoTemplate.aggregate(pipeline, Measure.class, FacetDTO.class).getMappedResults();

    return new PageImpl<>(
        results.get(0).getQueryResults(), pageable, results.get(0).getCount().size());
  }

  @Override
  public List<LibraryUsage> findLibraryUsageByLibraryName(String name) {
    LookupOperation lookupOperation = getLookupOperation();
    MatchOperation matchOperation =
      match(
        new Criteria()
          .andOperator(
            Criteria.where("includedLibraries.name").is(name),
            Criteria.where("active").is(true)));
    ProjectionOperation projectionOperation =
      project("version")
        .and("measureName")
        .as("name")
        .and("measureSet.owner")
        .as("owner")
        .andExclude("_id");
    UnwindOperation unwindOperation = unwind("owner");
    Aggregation aggregation =
      newAggregation(matchOperation, lookupOperation, projectionOperation, unwindOperation);
    return mongoTemplate
      .aggregate(aggregation, Measure.class, LibraryUsage.class)
      .getMappedResults();
  }
}
