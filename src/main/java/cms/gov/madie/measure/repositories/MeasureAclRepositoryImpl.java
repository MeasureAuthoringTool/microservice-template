package cms.gov.madie.measure.repositories;

import gov.cms.madie.models.access.RoleEnum;
import gov.cms.madie.models.measure.Measure;

import org.apache.commons.lang3.StringUtils;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.limit;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.skip;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;

@Repository
public class MeasureAclRepositoryImpl implements MeasureAclRepository {
  private final MongoTemplate mongoTemplate;

  public MeasureAclRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public Page<Measure> findMyActiveMeasures(String userId, Pageable pageable, String searchTerm) {
    // join measure and measure_set to lookup owner and ACL info
    LookupOperation lookupOperation =
        LookupOperation.newLookup()
            .from("measureSet")
            .localField("measureSetId")
            .foreignField("measureSetId")
            .as("measureSet");

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
                Criteria.where("measureSet.owner").is(userId),
                Criteria.where("measureSet.acls.userId")
                    .is(userId)
                    .and("measureSet.acls.roles")
                    .in(RoleEnum.SHARED_WITH));

    // combine measure and measure set criteria
    MatchOperation matchOperation =
        match(new Criteria().andOperator(measureCriteria, measureSetCriteria));

    Aggregation countAggregation = newAggregation(lookupOperation, matchOperation);

    Aggregation pageableAggregation =
        newAggregation(
            lookupOperation,
            matchOperation,
            sort(pageable.getSort()),
            skip(pageable.getOffset()),
            limit(pageable.getPageSize()));

    // TODO: it would be nice if we could get count and result both in single call.
    // I think we could by using Aggregation.facets but need more time to look into it
    long count =
        mongoTemplate
            .aggregate(countAggregation, Measure.class, Measure.class)
            .getMappedResults()
            .size();
    List<Measure> results =
        mongoTemplate
            .aggregate(pageableAggregation, Measure.class, Measure.class)
            .getMappedResults();

    return new PageImpl<>(results, pageable, count);
  }
}
