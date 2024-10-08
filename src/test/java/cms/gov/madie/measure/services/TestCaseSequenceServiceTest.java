package cms.gov.madie.measure.services;

import cms.gov.madie.measure.repositories.TestCaseSequenceRepository;
import gov.cms.madie.models.measure.TestCaseSequence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TestCaseSequenceServiceTest {
  @Mock MongoOperations mongoOperations;
  @Mock TestCaseSequenceRepository testCaseSequenceRepository;

  @InjectMocks TestCaseSequenceService testCaseSequenceService;

  @Test
  void generateSequence() {
    when(mongoOperations.findAndModify(
            any(Query.class), any(UpdateDefinition.class), any(FindAndModifyOptions.class), any()))
        .thenReturn(TestCaseSequence.builder().sequence(100).build());
    var result = testCaseSequenceService.generateSequence("measureId");
    assertEquals(100, result);
  }

  @Test
  void generateNewSequence() {
    when(mongoOperations.findAndModify(
            any(Query.class), any(UpdateDefinition.class), any(FindAndModifyOptions.class), any()))
        .thenReturn(null);
    var result = testCaseSequenceService.generateSequence("measureId");
    assertEquals(1, result);
  }

  @Test
  void resetSequence() {
    TestCaseSequence sequence = TestCaseSequence.builder().sequence(1).build();
    when(testCaseSequenceRepository.findById(anyString())).thenReturn(Optional.of(sequence));
    testCaseSequenceService.resetSequence("measureId");
    verify(testCaseSequenceRepository, times(1)).delete(sequence);
  }
}
