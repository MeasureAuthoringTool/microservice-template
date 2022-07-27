package cms.gov.madie.measure.repositories;

import com.mongodb.client.result.UpdateResult;
import gov.cms.madie.models.common.Action;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Slf4j
@RequiredArgsConstructor
@Repository
public class ActionLogRepositoryImpl implements ActionLogRepository {
  private final MongoTemplate mongoTemplate;

  @Override
  public boolean pushEvent(String targetId, Action action, String collection) {
    if (targetId == null || targetId.isEmpty() || action == null) {
      return false;
    }
    Update update = new Update();
    UpdateResult upsert =
        mongoTemplate.upsert(
            new Query(Criteria.where("targetId").is(targetId)),
            update.push("actions").value(action),
            collection);
    return upsert.getUpsertedId() != null || upsert.getModifiedCount() == 1;
  }
}
