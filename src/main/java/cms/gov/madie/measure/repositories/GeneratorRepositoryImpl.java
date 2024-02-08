package cms.gov.madie.measure.repositories;

import java.util.Optional;

import gov.cms.madie.models.measure.Generator;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
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
  public Optional<Generator> findAndModify(String sequenceName) {
    Generator counter =
        mongoOperations.findAndModify(
            query(where("name").is(sequenceName)),
            new Update().inc("curVal", 1),
            options().returnNew(true).upsert(true),
            Generator.class);
    return Optional.ofNullable(counter);
  }

  @Override
  public boolean existsBySequenceName(String sequenceName) {
    Query query = new Query(where("name").is(sequenceName));
    Generator test = mongoOperations.findOne(query, Generator.class);
    if (test != null) {
      return true;
    }
    return false;
  }
}
