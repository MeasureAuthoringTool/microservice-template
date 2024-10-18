package cms.gov.madie.measure.services;

import cms.gov.madie.measure.repositories.TestCaseSequenceRepository;
import gov.cms.madie.models.measure.TestCaseSequence;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Slf4j
@Service
@AllArgsConstructor
public class TestCaseSequenceService {
  private MongoOperations mongoOperations;
  private TestCaseSequenceRepository sequenceRepository;

  public int generateSequence(String measureId) {
    TestCaseSequence counter =
        mongoOperations.findAndModify(
            query(where("_id").is(measureId)),
            new Update().inc("sequence", 1),
            options().returnNew(true).upsert(true),
            TestCaseSequence.class);
    return Objects.isNull(counter) ? 1 : counter.getSequence();
  }

  public void resetSequence(String measureId) {
    Optional<TestCaseSequence> sequence = sequenceRepository.findById(measureId);
    sequence.ifPresent(
        testCaseSequence -> {
          sequenceRepository.delete(testCaseSequence);
          log.info("Reset sequence for test cases for measure [{}]", measureId);
        });
  }

  public void setSequence(String measureId, int sequenceNumber) {
    Optional<TestCaseSequence> sequence = sequenceRepository.findById(measureId);
    sequence.ifPresent(
        testCaseSequence -> {
          testCaseSequence.setSequence(sequenceNumber);
          sequenceRepository.save(testCaseSequence);
          log.info(
              "Update sequence for test cases for measure [{}], new sequence number is [{}]",
              measureId,
              sequenceNumber);
        });
    if (sequence.isEmpty()) {
      TestCaseSequence testCaseSequence =
          TestCaseSequence.builder().id(measureId).sequence(sequenceNumber).build();
      sequenceRepository.save(testCaseSequence);
    }
  }
}
