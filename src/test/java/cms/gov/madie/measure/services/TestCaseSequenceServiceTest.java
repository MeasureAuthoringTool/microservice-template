package cms.gov.madie.measure.services;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TestCaseSequenceServiceTest {
  @Mock MongoOperations mongoOperations;

  @InjectMocks TestCaseSequenceService testCaseSequenceService;

  @Test
  void generateSequence() {

    when(mongoOperations.findAndModify(
            any(Query.class), any(UpdateDefinition.class), any(FindAndModifyOptions.class), any()))
        .thenReturn(TestCaseSequence.builder().sequence(1).build());
    var result = testCaseSequenceService.generateSequence("measureId");
    assertEquals(1, result);
  }
}
