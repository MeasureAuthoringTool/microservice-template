package cms.gov.madie.measure.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureGroupTypes;
import gov.cms.madie.models.measure.MeasureMetaData;
import gov.cms.madie.models.measure.MeasureObservation;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.PopulationType;
import gov.cms.madie.models.measure.Stratification;
import gov.cms.madie.models.measure.TestCase;

@ExtendWith(MockitoExtension.class)
public class MeasureTransferServiceTest {

  @InjectMocks private MeasureTransferService measureTransferService;

  private Measure measure1;
  private Measure measure2;
  private Group group1;
  private TestCase testcase;

  @BeforeEach
  public void setUp() {
    measure1 =
        Measure.builder()
            .id("testMeasureId1")
            .measureSetId("testMeasureSetId1")
            .measureMetaData(MeasureMetaData.builder().draft(false).build())
            .lastModifiedAt(Instant.now())
            .build();

    measure2 =
        Measure.builder()
            .id("testMeasureId2")
            .measureSetId("testMeasureSetId1")
            .measureMetaData(MeasureMetaData.builder().draft(true).build())
            .lastModifiedAt(Instant.now().minus(1, ChronoUnit.DAYS))
            .build();

    Population population1 =
        Population.builder()
            .name(PopulationType.INITIAL_POPULATION)
            .definition("Initial Population")
            .build();
    Population population2 =
        Population.builder().name(PopulationType.DENOMINATOR).definition("Denominator").build();
    Population population3 =
        Population.builder()
            .name(PopulationType.DENOMINATOR_EXCLUSION)
            .definition("Denominator Exclusion")
            .build();
    Population population4 =
        Population.builder().name(PopulationType.NUMERATOR).definition("Numerator").build();

    MeasureObservation observation1 =
        MeasureObservation.builder().definition("Denominator Observation").build();

    Stratification stratification1 =
        Stratification.builder().cqlDefinition("Initial Population").build();

    group1 =
        Group.builder()
            .id("testGroupId1")
            .scoring("Proportion")
            .populationBasis("Encounter")
            .measureGroupTypes(Arrays.asList(MeasureGroupTypes.OUTCOME))
            .populations(List.of(population1, population2, population3, population4))
            .measureObservations(List.of(observation1))
            .stratifications(List.of(stratification1))
            .build();

    testcase = TestCase.builder().id("testCaseId").build();
  }

  @Test
  public void testOverwriteExistingMeasure() {
    Measure transferredMeasure = Measure.builder().build();
    measure1.setMeasureMetaData(MeasureMetaData.builder().draft(true).build());

    Measure overwrittenMeasure =
        measureTransferService.overwriteExistingMeasure(
            List.of(measure1, measure2), transferredMeasure);
    assertEquals("testMeasureId1", overwrittenMeasure.getId());
  }

  @Test
  public void testOverwriteExistingMeasureNotOverwrittenNoMetaData() {
    Measure transferredMeasure = Measure.builder().build();
    measure1.setMeasureMetaData(null);
    measure2.setMeasureMetaData(null);

    Measure overwrittenMeasure =
        measureTransferService.overwriteExistingMeasure(
            List.of(measure1, measure2), transferredMeasure);
    assertNull(overwrittenMeasure.getId());
  }

  @Test
  public void testOverwriteExistingMeasureNotOverwrittenNoDraft() {
    Measure transferredMeasure = Measure.builder().build();
    measure2.setMeasureMetaData(MeasureMetaData.builder().draft(false).build());

    Measure overwrittenMeasure =
        measureTransferService.overwriteExistingMeasure(
            List.of(measure1, measure2), transferredMeasure);
    assertNull(overwrittenMeasure.getId());
  }

  @Test
  public void testOverwriteExistingMeasureWithTestCases() {
    Measure transferredMeasure = Measure.builder().model("QDM").groups(List.of(group1)).build();
    measure1.setMeasureMetaData(MeasureMetaData.builder().draft(true).build());
    measure1.setGroups(List.of(group1));
    measure1.setTestCases(List.of(testcase));

    Measure overwrittenMeasure =
        measureTransferService.overwriteExistingMeasure(List.of(measure1), transferredMeasure);

    assertNotNull(overwrittenMeasure.getId());
    assertEquals(1, overwrittenMeasure.getTestCases().size());
  }
}
