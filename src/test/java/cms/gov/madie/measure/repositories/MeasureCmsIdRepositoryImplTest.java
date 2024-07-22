package cms.gov.madie.measure.repositories;

import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.measure.Measure;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@EnableMongoRepositories(basePackages = "com.gov.madie.measure.repository")
public class MeasureCmsIdRepositoryImplTest {
  @Mock MongoTemplate mongoTemplate;
  @InjectMocks MeasureCmsIdRepositoryImpl measureCmsIdRepositoryImpl;

  @Test
  public void testFindMeasuresWhenMeasuresExistsWithGivenModelAndCmsId() {
    Measure qiCoreMeasure =
        Measure.builder().model(ModelType.QI_CORE.getValue()).measureSetId("NewIDIDID").build();
    when(mongoTemplate.aggregate(any(Aggregation.class), (Class<?>) any(), any()))
        .thenReturn(new AggregationResults<>(List.of(qiCoreMeasure), new Document()));

    List<Measure> allMeasuresByModelAndCmsId =
        measureCmsIdRepositoryImpl.findAllByModelAndCmsId(ModelType.QI_CORE.getValue(), 12);
    assertThat(
        allMeasuresByModelAndCmsId.get(0).getModel(), is(equalTo(ModelType.QI_CORE.getValue())));
    assertThat(allMeasuresByModelAndCmsId.get(0).getMeasureSetId(), is(equalTo("NewIDIDID")));
    assertThat(allMeasuresByModelAndCmsId.size(), is(1));
  }

  @Test
  public void testFindMeasuresWhenMeasuresDoesNotExistsWithGivenModelAndCmsId() {
    when(mongoTemplate.aggregate(any(Aggregation.class), (Class<?>) any(), any()))
        .thenReturn(new AggregationResults<>(List.of(), new Document()));

    List<Measure> allMeasuresByModelAndCmsId =
        measureCmsIdRepositoryImpl.findAllByModelAndCmsId(ModelType.QI_CORE.getValue(), 12);
    assertThat(allMeasuresByModelAndCmsId.size(), is(0));
  }
}
