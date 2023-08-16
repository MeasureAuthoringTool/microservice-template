package cms.gov.madie.measure.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.MeasureScoring;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.PopulationType;

@ExtendWith(MockitoExtension.class)
public class TestCaseServiceUtilTest {
	
	@InjectMocks private TestCaseServiceUtil testCaseServiceUtil;
	private Population population1;
	private Population population2;
	private Population population3;
	private Population population4;
	private Population population5;
	private Group group;

	@Test
  public void testGetGroupsWithValidPopulationsReturnsNullWithNullInput() {
		List<Group> changedGroups =  testCaseServiceUtil.getGroupsWithValidPopulations(null);
		assertThat(changedGroups, is(nullValue()));
	}
	
	@Test
  public void testGetGroupsWithValidPopulationsRemovePopulationsWithoutDefinition() {	
		population1 = Population.builder().name(PopulationType.INITIAL_POPULATION).definition("Initial Population").build();
    population2 = Population.builder().name(PopulationType.DENOMINATOR).build();
    population3 = Population.builder().name(PopulationType.DENOMINATOR_EXCLUSION).build();
    population4 = Population.builder().name(PopulationType.NUMERATOR).build();
    population5 = Population.builder().name(PopulationType.NUMERATOR_EXCLUSION).build();
    group =
        Group.builder()
            .id("testGroupId")
            .scoring(MeasureScoring.COHORT.name())
            .populationBasis("Boolean")
            .populations(List.of(population1, population2, population3, population4, population5))
            .build();
    List<Group> changedGroups =  testCaseServiceUtil.getGroupsWithValidPopulations(List.of(group));
    assertNotNull(changedGroups);
    assertThat(changedGroups.size(), is(1));
    assertNotNull(changedGroups.get(0));
    assertNotNull(changedGroups.get(0).getPopulations());
    assertThat(changedGroups.get(0).getPopulations().size(), is(1));
	}
	
	@Test
  public void testGetGroupsWithValidPopulationsNoRemoval() {	
		population1 = Population.builder().name(PopulationType.INITIAL_POPULATION).definition("Initial Population").build();
    population2 = Population.builder().name(PopulationType.DENOMINATOR).definition("Denominator").build();
    population3 = Population.builder().name(PopulationType.DENOMINATOR_EXCLUSION).definition("Denominator Exclusion").build();
    population4 = Population.builder().name(PopulationType.NUMERATOR).definition("Numerator").build();
    population5 = Population.builder().name(PopulationType.NUMERATOR_EXCLUSION).definition("Numerator Exclusion").build();
    group =
        Group.builder()
            .id("testGroupId")
            .scoring(MeasureScoring.COHORT.name())
            .populationBasis("Boolean")
            .populations(List.of(population1, population2, population3, population4, population5))
            .build();
    List<Group> changedGroups =  testCaseServiceUtil.getGroupsWithValidPopulations(List.of(group));
    assertNotNull(changedGroups);
    assertThat(changedGroups.size(), is(1));
    assertNotNull(changedGroups.get(0));
    assertNotNull(changedGroups.get(0).getPopulations());
    assertThat(changedGroups.get(0).getPopulations().size(), is(5));
	}
}
