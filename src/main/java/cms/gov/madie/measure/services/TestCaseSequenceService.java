package cms.gov.madie.measure.services;

import gov.cms.madie.models.measure.TestCaseSequence;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Slf4j
@Service
@AllArgsConstructor
public class TestCaseSequenceService {
  private MongoOperations mongoOperations;

  public int generateSequence(String measureId) {
    TestCaseSequence counter =
        mongoOperations.findAndModify(
            query(where("_id").is(measureId)),
            new Update().inc("sequence", 1),
            options().returnNew(true).upsert(true),
            TestCaseSequence.class);
    return Objects.isNull(counter) ? 1 : counter.getSequence();
  }
}
