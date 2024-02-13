package cms.gov.madie.measure.repositories;

import java.util.Objects;

import gov.cms.madie.models.measure.Generator;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Repository
public class GeneratorRepositoryImpl implements GeneratorRepository {

  private final MongoOperations mongoOperations;

  public GeneratorRepositoryImpl(MongoOperations mongoOperations) {
    this.mongoOperations = mongoOperations;
  }

  @Override
  public int findAndModify(String sequenceName) {
    Generator counter =
        mongoOperations.findAndModify(
            query(where("_id").is(sequenceName)),
            new Update().inc("currentValue", 1),
            options().returnNew(true).upsert(true),
            Generator.class);
    return Objects.isNull(counter) ? 1 : counter.getCurrentValue();
  }
}
