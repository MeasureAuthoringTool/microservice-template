package cms.gov.madie.measure.repositories;

import com.mongodb.client.result.UpdateResult;
import gov.cms.madie.models.common.Action;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionLogRepositoryImplTest {

  @Mock MongoTemplate mongoTemplate;

  @InjectMocks ActionLogRepositoryImpl actionLogRepository;

  @Test
  void returnsFalseForNullTargetId() {
    boolean output = actionLogRepository.pushEvent(null, Action.builder().build(), "COL1");
    assertThat(output, is(false));
  }

  @Test
  void returnsFalseForEmptyTargetId() {
    boolean output = actionLogRepository.pushEvent("", Action.builder().build(), "COL1");
    assertThat(output, is(false));
  }

  @Test
  void returnsFalseForNullAction() {
    boolean output = actionLogRepository.pushEvent("TARGET_ID", null, "COL1");
    assertThat(output, is(false));
  }

  @Test
  void returnsTrueForValidInputs() {
    when(mongoTemplate.upsert(any(Query.class), any(Update.class), anyString()))
        .thenReturn(UpdateResult.acknowledged(1, 1L, null));
    boolean output = actionLogRepository.pushEvent("TARGET_ID", Action.builder().build(), "COL1");
    assertThat(output, is(true));
  }

  @Test
  void returnsFalseForValidInputsNoUpsert() {
    when(mongoTemplate.upsert(any(Query.class), any(Update.class), anyString()))
        .thenReturn(UpdateResult.acknowledged(1, 0L, null));
    boolean output = actionLogRepository.pushEvent("TARGET_ID", Action.builder().build(), "COL1");
    assertThat(output, is(false));
  }
}
