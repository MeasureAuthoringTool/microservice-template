package cms.gov.madie.measure.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.common.Version;

@ExtendWith(MockitoExtension.class)
@EnableMongoRepositories(basePackages = "com.gov.madie.measure.repository")
public class MeasureVersionRepositoryImplTest {

  @Mock MongoTemplate mongoTemplate;

  @InjectMocks MeasureVersionRepositoryImpl measureVersionRepository;

  @Test
  public void testFindMaxVersionByMeasureSetIdSuccess() {
    Measure measure = Measure.builder().measureSetId("testMeasureSetId").build();
    Version version = Version.builder().major(1).minor(2).revisionNumber(3).build();
    measure.setVersion(version);

    when(mongoTemplate.findOne(any(Query.class), any())).thenReturn(measure);

    Optional<Version> foundVersion =
        measureVersionRepository.findMaxVersionByMeasureSetId("testMeasureSetId");
    assertEquals(foundVersion.get().getMajor(), 1);
    assertEquals(foundVersion.get().getMinor(), 2);
    assertEquals(foundVersion.get().getRevisionNumber(), 3);
  }

  @Test
  public void testFindMaxVersionByMeasureSetIdNullResult() {
    when(mongoTemplate.findOne(any(Query.class), any())).thenReturn(null);

    Optional<Version> foundVersion =
        measureVersionRepository.findMaxVersionByMeasureSetId("testMeasureSetId");

    assertTrue(foundVersion.isEmpty());
  }

  @Test
  public void testFindMaxVersionByMeasureSetIdNullVersionResult() {
    Measure measure = Measure.builder().measureSetId("testMeasureSetId").build();
    measure.setVersion(null);

    when(mongoTemplate.findOne(any(Query.class), any())).thenReturn(measure);

    Optional<Version> foundVersion =
        measureVersionRepository.findMaxVersionByMeasureSetId("testMeasureSetId");

    assertTrue(foundVersion.isEmpty());
  }

  @Test
  public void testFindMaxMinorVersionByMeasureSetIdAndVersionMajorSuccess() {
    Measure measure = Measure.builder().measureSetId("testMeasureSetId").build();
    Version version = Version.builder().major(1).minor(2).revisionNumber(3).build();
    measure.setVersion(version);

    when(mongoTemplate.findOne(any(Query.class), any())).thenReturn(measure);

    Optional<Version> foundVersion =
        measureVersionRepository.findMaxMinorVersionByMeasureSetIdAndVersionMajor(
            "testMeasureSetId", 1);

    assertEquals(foundVersion.get().getMajor(), 1);
    assertEquals(foundVersion.get().getMinor(), 2);
    assertEquals(foundVersion.get().getRevisionNumber(), 3);
  }

  @Test
  public void testFindMaxMinorVersionByMeasureSetIdAndVersionMajorNullResult() {
    when(mongoTemplate.findOne(any(Query.class), any())).thenReturn(null);

    Optional<Version> foundVersion =
        measureVersionRepository.findMaxMinorVersionByMeasureSetIdAndVersionMajor(
            "testMeasureSetId", 1);

    assertTrue(foundVersion.isEmpty());
  }

  @Test
  public void testFindMaxMinorVersionByMeasureSetIdAndVersionMajorNullVersionResult() {
    Measure measure = Measure.builder().measureSetId("testMeasureSetId").build();
    measure.setVersion(null);

    when(mongoTemplate.findOne(any(Query.class), any())).thenReturn(measure);

    Optional<Version> foundVersion =
        measureVersionRepository.findMaxMinorVersionByMeasureSetIdAndVersionMajor(
            "testMeasureSetId", 1);

    assertTrue(foundVersion.isEmpty());
  }

  @Test
  public void testFindMaxRevisionNumberByMeasureSetIdAndVersionMajorAndMinorSuccess() {
    Measure measure = Measure.builder().measureSetId("testMeasureSetId").build();
    Version version = Version.builder().major(1).minor(2).revisionNumber(3).build();
    measure.setVersion(version);

    when(mongoTemplate.findOne(any(Query.class), any())).thenReturn(measure);

    Optional<Version> foundVersion =
        measureVersionRepository.findMaxRevisionNumberByMeasureSetIdAndVersionMajorAndMinor(
            "testMeasureSetId", 1, 2);

    assertEquals(foundVersion.get().getMajor(), 1);
    assertEquals(foundVersion.get().getMinor(), 2);
    assertEquals(foundVersion.get().getRevisionNumber(), 3);
  }

  @Test
  public void testFindMaxRevisionNumberByMeasureSetIdAndVersionMajorAndMinorNullResult() {
    when(mongoTemplate.findOne(any(Query.class), any())).thenReturn(null);

    Optional<Version> foundVersion =
        measureVersionRepository.findMaxRevisionNumberByMeasureSetIdAndVersionMajorAndMinor(
            "testMeasureSetId", 1, 2);

    assertTrue(foundVersion.isEmpty());
  }

  @Test
  public void testFindMaxRevisionNumberByMeasureSetIdAndVersionMajorAndMinorNullVersionResult() {
    Measure measure = Measure.builder().measureSetId("testMeasureSetId").build();
    measure.setVersion(null);

    when(mongoTemplate.findOne(any(Query.class), any())).thenReturn(measure);

    Optional<Version> foundVersion =
        measureVersionRepository.findMaxRevisionNumberByMeasureSetIdAndVersionMajorAndMinor(
            "testMeasureSetId", 1, 2);

    assertTrue(foundVersion.isEmpty());
  }
}
