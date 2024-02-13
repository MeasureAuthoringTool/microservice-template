package cms.gov.madie.measure.repositories;

import gov.cms.madie.models.measure.Generator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneratorRepositoryImplTest {

  @Mock MongoOperations mongoOperations;

  @InjectMocks GeneratorRepositoryImpl generatorRepository;

  @Test
  void findAndModify() {

    when(mongoOperations.findAndModify(
            any(Query.class), any(UpdateDefinition.class), any(FindAndModifyOptions.class), any()))
        .thenReturn(Generator.builder().currentValue(1).build());
    var result = generatorRepository.findAndModify("cms_id");
    assertEquals(1, result);
  }
}
