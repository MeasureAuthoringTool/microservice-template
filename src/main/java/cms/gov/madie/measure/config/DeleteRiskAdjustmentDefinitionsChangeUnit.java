package cms.gov.madie.measure.config;

import gov.cms.madie.models.measure.Measure;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Slf4j
@ChangeUnit(id = "delete-risk-adjustment-descriptions", order = "2", author = "madie_dev")
public class DeleteRiskAdjustmentDefinitionsChangeUnit {
  @Execution
  public void deleteAdjustmentDefinitions(MongoOperations mongoOperations) {
    Query query = new Query();
    Update update = new Update().unset("riskAdjustments.$[].description");

    BulkOperations bulkOperations =
        mongoOperations.bulkOps(BulkOperations.BulkMode.UNORDERED, Measure.class);
    bulkOperations.updateMulti(query, update);
    bulkOperations.execute();
  }

  @RollbackExecution
  public void rollbackExecution() {
    log.debug("Entering rollbackExecution()");
  }
}
