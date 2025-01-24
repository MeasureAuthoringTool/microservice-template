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

import java.util.ArrayList;
import java.util.List;

import static org.codehaus.plexus.util.StringUtils.isNumeric;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
public class MeasureSearchServiceImpl implements MeasureSearchService {
  private final MongoTemplate mongoTemplate;

  public MeasureSearchServiceImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  private LookupOperation getLookupOperation() {
    return LookupOperation.newLookup()
        .from("measureSet")
        .localField("measureSetId")
        .foreignField("measureSetId")
        .as("measureSet");
  }

  private void appendMeasureNameAndEcqmTitleCriteria(
      Criteria measureCriteria, MeasureSearchCriteria measureSearchCriteria) {
    measureCriteria.andOperator(
        new Criteria()
            .orOperator(
                Criteria.where("measureName").regex(measureSearchCriteria.getSearchField(), "i"),
                Criteria.where("ecqmTitle").regex(measureSearchCriteria.getSearchField(), "i")));
  }

  private void appendAdditionalSearchCriteriaOmittingcmsId(
      Criteria measureCriteria, MeasureSearchCriteria measureSearchCriteria) {
    // Ensure optionalSearchProperties exists and isn’t empty
    if (CollectionUtils.isNotEmpty(measureSearchCriteria.getOptionalSearchProperties())) {
      // Filter out "cmsId" if it exists
      List<String> filteredProperties =
          measureSearchCriteria.getOptionalSearchProperties().stream()
              .filter(property -> !"cmsId".equalsIgnoreCase(property))
              .toList();

      // Build the orOperator for the remaining properties
      List<Criteria> orConditions = new ArrayList<>();
      for (String property : filteredProperties) {
        orConditions.add(
            Criteria.where(property).regex(measureSearchCriteria.getSearchField(), "i"));
      }
      if (!orConditions.isEmpty()) {
        // if it's a single and condition, we only want that criteria
        if (measureSearchCriteria.getOptionalSearchProperties().size() == 1) {
          measureCriteria.andOperator(orConditions.toArray(new Criteria[0]));
        } else {
          // if it's multiple conditions we want any of the conditions to be a match
          measureCriteria.orOperator(orConditions.toArray(new Criteria[0]));
        }
      }
    }
  }

  // First get All Active measures
  // if searchField string is given then search for the searchField string in measureName or eCQM
  // title
  // If model is provided filter out those measures based on Model
  // if draft status is provided then filter out them based on draft value
  // if filterByCurrentUser = true then filter measures owned by user or shared with
  @Override
  public Page<MeasureListDTO> searchMeasuresByCriteria(
      String userId,
      Pageable pageable,
      MeasureSearchCriteria measureSearchCriteria,
      boolean filterByCurrentUser) {
    // join measure and measure_set to lookup owner and ACL info
    LookupOperation lookupOperation = getLookupOperation();

    // prepare measure search criteria
    Criteria measureCriteria = Criteria.where("active").is(true);
    if (measureSearchCriteria != null) {
      // If query is given, search for the query string in measureName and ecqmTitle (or if
      // optionalFilter.includes(measureName)
      if (StringUtils.isNotBlank(measureSearchCriteria.getSearchField())
          && CollectionUtils.isEmpty(measureSearchCriteria.getOptionalSearchProperties())) {
        appendMeasureNameAndEcqmTitleCriteria(measureCriteria, measureSearchCriteria);
      }
      // optional query provided
      if (StringUtils.isNotBlank(measureSearchCriteria.getSearchField())
          && CollectionUtils.isNotEmpty(measureSearchCriteria.getOptionalSearchProperties())) {
        appendAdditionalSearchCriteriaOmittingcmsId(measureCriteria, measureSearchCriteria);
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
      if (CollectionUtils.isNotEmpty(measureSearchCriteria.getExcludeByMeasureIds())) {
        measureCriteria.and("_id").nin(measureSearchCriteria.getExcludeByMeasureIds());
      }
    }

    // prepare measure set search criteria(user is either owner or shared with)
    Criteria measureSetCriteria = new Criteria();
    if (filterByCurrentUser) {
      List<Criteria> measureSetOrCriteriaList = new ArrayList<>();
      List<Criteria> measureSetAndCriteriaList = new ArrayList<>();
      measureSetOrCriteriaList.add(
          Criteria.where("measureSet.owner").regex("^\\Q" + userId + "\\E$", "i"));
      measureSetOrCriteriaList.add(
          Criteria.where("measureSet.acls.userId")
              .regex("^\\Q" + userId + "\\E$", "i")
              .and("measureSet.acls.roles")
              .in(RoleEnum.SHARED_WITH));
      if (measureSearchCriteria != null) {
        if (measureSearchCriteria.getOptionalSearchProperties() != null
            && !measureSearchCriteria.getOptionalSearchProperties().isEmpty()
            && measureSearchCriteria.getOptionalSearchProperties().contains("cmsId")) {
          // When cmsId is provided, we want to either do an and or an OR conditional.
          // If we have more than one condition, we want to do an OR condition.
          // If it is provided as the only condition, it should be an AND condition
          String searchField = measureSearchCriteria.getSearchField();
          if (isNumeric(searchField)) {
            int number = Integer.parseInt(searchField);
            if (measureSearchCriteria.getOptionalSearchProperties().size() > 1) {
              measureSetOrCriteriaList.add(Criteria.where("measureSet.cmsId").is(number));
            } else {
              measureSetAndCriteriaList.add(Criteria.where("measureSet.cmsId").is(number));
            }
          }
        }
      }
      // generate the query now that we know weverything we need
      measureSetCriteria =
          measureSetCriteria.orOperator(measureSetOrCriteriaList.toArray(new Criteria[0]));
      // Apply AND conditions if there are any
      if (!measureSetAndCriteriaList.isEmpty()) {
        measureSetCriteria =
            measureSetCriteria.andOperator(measureSetAndCriteriaList.toArray(new Criteria[0]));
      }
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
