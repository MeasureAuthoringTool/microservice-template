package cms.gov.madie.measure.repositories;

import gov.cms.madie.models.measure.Measure;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
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

  private Measure measure1;
  private Measure measure2;
  private Measure measure3;
  private Measure measure4;
  private Measure measure5;

  @BeforeEach
  void setup() {
    measure1 = Measure.builder().id("1").ecqmTitle("test measure 1").measureSetId("1-1").build();
    measure2 = Measure.builder().id("2").ecqmTitle("test measure 2").measureSetId("2-2").build();
    measure3 = Measure.builder().id("3").measureSetId("3-3").build();
    measure4 = Measure.builder().id("4").measureSetId("4-4").build();
    measure5 = Measure.builder().id("5").measureSetId("5-5").build();
  }

  @Test
  public void testFindMyActiveMeasures() {
    // page size 3 from 0-2
    PageRequest pageRequest = PageRequest.of(0, 3);
    List<Measure> allMeasures = List.of(measure1, measure2, measure3, measure4, measure5);

    AggregationResults allResults = new AggregationResults<>(allMeasures, new Document());
    AggregationResults pagedResults =
        new AggregationResults<>(List.of(measure1, measure2, measure3), new Document());

    when(mongoTemplate.aggregate(any(Aggregation.class), (Class<?>) any(), any()))
        .thenReturn(allResults)
        .thenReturn(pagedResults);

    Page<Measure> page = measureAclRepository.findMyActiveMeasures("john", pageRequest, null);
    assertEquals(page.getTotalElements(), 5);
    assertEquals(page.getTotalPages(), 2);
    assertEquals(page.getContent().size(), 3);
    List<Measure> page1Measures = page.getContent();
    assertEquals(page1Measures.get(0).getId(), measure1.getId());
    assertEquals(page1Measures.get(1).getId(), measure2.getId());
    assertEquals(page1Measures.get(2).getId(), measure3.getId());
  }

  @Test
  public void testFindMyActiveMeasuresWithSearchTerm() {
    PageRequest pageRequest = PageRequest.of(0, 3);
    AggregationResults allResults =
        new AggregationResults<>(List.of(measure1, measure2), new Document());
    AggregationResults pagedResults =
        new AggregationResults<>(List.of(measure1, measure2), new Document());

    when(mongoTemplate.aggregate(any(Aggregation.class), (Class<?>) any(), any()))
        .thenReturn(allResults)
        .thenReturn(pagedResults);

    Page<Measure> page =
        measureAclRepository.findMyActiveMeasures("john", pageRequest, "test measure");
    assertEquals(page.getTotalElements(), 2);
    assertEquals(page.getTotalPages(), 1);
    assertEquals(page.getContent().size(), 2);
    List<Measure> page1Measures = page.getContent();
    assertEquals(page1Measures.get(0).getEcqmTitle(), measure1.getEcqmTitle());
    assertEquals(page1Measures.get(1).getEcqmTitle(), measure2.getEcqmTitle());
  }
}
