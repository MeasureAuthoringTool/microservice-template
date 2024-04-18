package cms.gov.madie.measure.repositories;

import cms.gov.madie.measure.dto.FacetDTO;
import gov.cms.madie.models.dto.MeasureList;
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

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@EnableMongoRepositories(basePackages = "com.gov.madie.measure.repository")
public class MeasureAclRepositoryImplTest {

  @Mock MongoTemplate mongoTemplate;

  @InjectMocks MeasureAclRepositoryImpl measureAclRepository;

  private MeasureList measure1;
  private MeasureList measure2;
  private MeasureList measure3;
  private MeasureList measure4;
  private MeasureList measure5;

  @BeforeEach
  void setup() {
    measure1 =
        MeasureList.builder().id("1").ecqmTitle("test measure 1").measureSetId("1-1").build();
    measure2 =
        MeasureList.builder().id("2").ecqmTitle("test measure 2").measureSetId("2-2").build();
    measure3 = MeasureList.builder().id("3").measureSetId("3-3").build();
    measure4 = MeasureList.builder().id("4").measureSetId("4-4").build();
    measure5 = MeasureList.builder().id("5").measureSetId("5-5").build();
  }

  @Test
  public void testFindMyActiveMeasures() {
    // page size 3 from 0-2
    PageRequest pageRequest = PageRequest.of(0, 3);
    List<MeasureList> allMeasures = List.of(measure1, measure2, measure3, measure4, measure5);

    FacetDTO facetDTO =
        FacetDTO.builder()
            .queryResults(List.of(measure1, measure2, measure3))
            .count(Arrays.asList(allMeasures.toArray()))
            .build();

    AggregationResults pagedResults = new AggregationResults<>(List.of(facetDTO), new Document());

    when(mongoTemplate.aggregate(any(Aggregation.class), (Class<?>) any(), any()))
        .thenReturn(pagedResults);

    Page<MeasureList> page = measureAclRepository.findMyActiveMeasures("john", pageRequest, null);
    assertEquals(page.getTotalElements(), 5);
    assertEquals(page.getTotalPages(), 2);
    assertEquals(page.getContent().size(), 3);
    List<MeasureList> page1Measures = page.getContent();
    assertEquals(page1Measures.get(0).getId(), measure1.getId());
    assertEquals(page1Measures.get(1).getId(), measure2.getId());
    assertEquals(page1Measures.get(2).getId(), measure3.getId());
  }

  @Test
  public void testFindMyActiveMeasuresWithSearchTerm() {
    PageRequest pageRequest = PageRequest.of(0, 3);

    FacetDTO facetDTO =
        FacetDTO.builder().queryResults(List.of(measure1, measure2)).count(List.of(1, 2)).build();
    AggregationResults pagedResults = new AggregationResults<>(List.of(facetDTO), new Document());

    when(mongoTemplate.aggregate(any(Aggregation.class), (Class<?>) any(), any()))
        .thenReturn(pagedResults);

    Page<MeasureList> page =
        measureAclRepository.findMyActiveMeasures("john", pageRequest, "test measure");
    assertEquals(page.getTotalElements(), 2);
    assertEquals(page.getTotalPages(), 1);
    assertEquals(page.getContent().size(), 2);
    List<MeasureList> page1Measures = page.getContent();
    assertEquals(page1Measures.get(0).getEcqmTitle(), measure1.getEcqmTitle());
    assertEquals(page1Measures.get(1).getEcqmTitle(), measure2.getEcqmTitle());
  }
}
