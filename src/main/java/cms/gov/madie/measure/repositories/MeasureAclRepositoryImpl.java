package cms.gov.madie.measure.repositories;

import cms.gov.madie.measure.dto.FacetDTO;
import cms.gov.madie.measure.dto.MeasureListDTO;
import cms.gov.madie.measure.dto.MeasureSearchCriteria;
import gov.cms.madie.models.access.RoleEnum;
import gov.cms.madie.models.dto.LibraryUsage;
import gov.cms.madie.models.measure.Measure;

import org.apache.commons.collections4.CollectionUtils;
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

  // First get All Active measures
  // if query string is given then search for the query string in measureName and eCQM title
  // If model is provided filter out those measures based on Model
  // if draft status is provided then filter out them based on draft value
  // if filterByCurrentUser = true then filter measures owned by user or shared with
  @Override
  public Page<MeasureListDTO> findActiveMeasures(
      String userId,
      Pageable pageable,
      MeasureSearchCriteria measureSearchCriteria,
      boolean filterByCurrentUser) {
    // join measure and measure_set to lookup owner and ACL info
    LookupOperation lookupOperation = getLookupOperation();

    // prepare measure search criteria
    Criteria measureCriteria = Criteria.where("active").is(true);

    if (measureSearchCriteria != null) {
      // If query is given, search for the query string in measureName and ecqmTitle
      if (StringUtils.isNotBlank(measureSearchCriteria.getQuery())) {
        measureCriteria.andOperator(
            new Criteria()
                .orOperator(
                    Criteria.where("measureName").regex(measureSearchCriteria.getQuery(), "i"),
                    Criteria.where("ecqmTitle").regex(measureSearchCriteria.getQuery(), "i")));
      }

      // If model is provided, filter out those measures with that model
      if (StringUtils.isNotBlank(measureSearchCriteria.getModel())) {
        measureCriteria.and("model").is(measureSearchCriteria.getModel());
      }

      // If draft is provided, filter measures based on MeasureMetaData.draft
      if (measureSearchCriteria.getDraft() != null) {
        measureCriteria.and("measureMetaData.draft").is(measureSearchCriteria.getDraft());
      }

      // If excludeMeasures is not empty, exclude those measures by their IDs
      if (CollectionUtils.isNotEmpty(measureSearchCriteria.getExcludeMeasures())) {
        measureCriteria.and("_id").nin(measureSearchCriteria.getExcludeMeasures());
      }
    }

    // prepare measure set search criteria(user is either owner or shared with)
    Criteria measureSetCriteria = new Criteria();
    if (filterByCurrentUser) {
      measureSetCriteria =
          new Criteria()
              .orOperator(
                  Criteria.where("measureSet.owner").regex("^\\Q" + userId + "\\E$", "i"),
                  Criteria.where("measureSet.acls.userId")
                      .regex("^\\Q" + userId + "\\E$", "i")
                      .and("measureSet.acls.roles")
                      .in(RoleEnum.SHARED_WITH));
    }

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
