package cms.gov.madie.measure.changes;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

import cms.gov.madie.measure.models.Measure;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.RequiredArgsConstructor;

@ChangeUnit(id = "CreateMeasureLibraryNameIndexChangeUnit", order = "0001")
@RequiredArgsConstructor
public class CreateMeasureLibraryNameIndexChangeUnit {

  private final MongoTemplate template;

  @Execution
  public void executeChange() {
    template
        .indexOps(Measure.class)
        .ensureIndex(new Index().unique().on("cqlLibraryName", Sort.Direction.ASC));
  }

  @RollbackExecution
  public void rollbackChange() {
    // Do nothing
  }
}
