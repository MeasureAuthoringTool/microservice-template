package cms.gov.madie.measure.config;

import java.util.List;

import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.nimbusds.oauth2.sdk.util.CollectionUtils;

import cms.gov.madie.measure.repositories.MeasureRepository;
import gov.cms.madie.models.measure.Measure;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChangeUnit(id = "delete_sde_description_from_defDescPair", order = "1", author = "madie_dev")
public class DeleteSupplementalDataDescriptionChangeUnit {

  @Execution
  public void deleteSdeDescriptionFromDefDescPair(
      MongoOperations mongoOperations, MeasureRepository measureRepository) {

    List<Measure> measureList = measureRepository.findAll();
    if (CollectionUtils.isNotEmpty(measureList)) {
      measureList.stream()
          .filter(measure -> CollectionUtils.isNotEmpty(measure.getSupplementalData()))
          .forEach(
              measure -> {
                Query query = new Query();
                Update update = new Update().unset("supplementalData.$[].description");

                BulkOperations bulkOperations =
                    mongoOperations.bulkOps(BulkOperations.BulkMode.UNORDERED, Measure.class);
                bulkOperations.updateOne(query, update);
                bulkOperations.execute();
              });
    }
  }

  @RollbackExecution
  public void rollbackExecution() {
    log.debug("Entering rollbackExecution()");
  }
}
