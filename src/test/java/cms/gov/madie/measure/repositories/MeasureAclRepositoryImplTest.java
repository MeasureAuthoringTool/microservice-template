package cms.gov.madie.measure.repositories;

import gov.cms.madie.models.measure.Measure;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@EnableMongoRepositories(basePackages = "com.gov.madie.measure.repository")
public class MeasureAclRepositoryImplTest {

  @Mock MongoTemplate mongoTemplate;

  @InjectMocks MeasureAclRepositoryImpl measureAclRepository;

  @Test
  public void testFindMyActiveMeasures() {
    // page size 3 from 0-2
    PageRequest pageRequest = PageRequest.of(0, 3);
    // Total measures: 5(2 pages total)
    Measure measure1 = Measure.builder().id("1").measureSetId("1-1").build();
    Measure measure2 = Measure.builder().id("2").measureSetId("2-2").build();
    Measure measure3 = Measure.builder().id("3").measureSetId("3-3").build();
    Measure measure4 = Measure.builder().id("4").measureSetId("4-4").build();
    Measure measure5 = Measure.builder().id("5").measureSetId("5-5").build();
    List<Measure> allMeasures = List.of(measure1, measure2, measure3, measure4, measure5);

    AggregationResults allResults = new AggregationResults<>(allMeasures, new Document());
    AggregationResults pagedResults =
        new AggregationResults<>(List.of(measure1, measure2, measure3), new Document());

    when(mongoTemplate.aggregate(any(Aggregation.class), (Class<?>) any(), any()))
        .thenReturn(allResults)
        .thenReturn(pagedResults);

    Page<Measure> page = measureAclRepository.findMyActiveMeasures("john", pageRequest);
    assertEquals(page.getTotalElements(), 5);
    assertEquals(page.getTotalPages(), 2);
    assertEquals(page.getContent().size(), 3);
    List<Measure> page1Measures = page.getContent();
    assertEquals(page1Measures.get(0).getId(), measure1.getId());
    assertEquals(page1Measures.get(1).getId(), measure2.getId());
    assertEquals(page1Measures.get(2).getId(), measure3.getId());
  }
}
