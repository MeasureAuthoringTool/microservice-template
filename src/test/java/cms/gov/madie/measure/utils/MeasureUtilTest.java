package cms.gov.madie.measure.utils;

import cms.gov.madie.measure.exceptions.InvalidReturnTypeException;
import cms.gov.madie.measure.exceptions.UnauthorizedException;
import cms.gov.madie.measure.services.MeasureService;
import cms.gov.madie.measure.validations.CqlDefinitionReturnTypeValidator;
import cms.gov.madie.measure.validations.CqlObservationFunctionValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import gov.cms.madie.models.access.AclSpecification;
import gov.cms.madie.models.access.RoleEnum;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureErrorType;
import gov.cms.madie.models.measure.Population;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeasureUtilTest {

  @Mock
  private CqlDefinitionReturnTypeValidator cqlDefinitionReturnTypeValidator;
  @Mock
  private CqlObservationFunctionValidator cqlObservationFunctionValidator;

  @InjectMocks
  private MeasureUtil measureUtil;
  private Measure measure;

  @BeforeEach
  public void setUp() {
    measure =
        Measure.builder()
            .active(true)
            .id("xyz-p13r-13ert")
            .cql("test cql")
            .measureSetId("IDIDID")
            .measureName("MSR01")
            .version(new Version(0, 0, 1))
            .createdAt(Instant.now())
            .createdBy("test user")
            .lastModifiedAt(Instant.now())
            .lastModifiedBy("test user")
            .build();
  }

  @Test
  public void testValidateAllMeasureGroupReturnTypesReturnsNullForNullInput() {
    Measure output = measureUtil.validateAllMeasureGroupReturnTypes(null);
    assertThat(output, is(nullValue()));
  }

  @Test
  public void testValidateAllMeasureGroupReturnTypesReturnsNoErrorsForNoGroups() {
    Measure measure = Measure.builder().elmJson("{}").groups(List.of()).build();
    Measure output = measureUtil.validateAllMeasureGroupReturnTypes(measure);
    assertThat(output, is(notNullValue()));
    assertThat(output.getErrors(), is(notNullValue()));
    assertThat(output.getErrors().isEmpty(), is(true));
  }

  @Test
  public void testValidateAllMeasureGroupReturnTypesReturnsMeasureNoErrors() throws Exception {
    Measure measure = Measure.builder().elmJson("{}")
        .groups(List.of(
            Group.builder().id("Group1").populations(null).build(),
            Group.builder().id("Group2").populations(List.of(
                Population.builder().definition("").build(),
                Population.builder().definition("GOOD DEFINE HERE").build()
            )).build()
        ))
        .build();
    doNothing()
        .when(cqlDefinitionReturnTypeValidator).validateCqlDefinitionReturnTypes(any(Group.class), anyString());
    doNothing()
        .when(cqlObservationFunctionValidator).validateObservationFunctions(any(Group.class), anyString());

    Measure output = measureUtil.validateAllMeasureGroupReturnTypes(measure);
    assertThat(output, is(notNullValue()));
    assertThat(output.getErrors(), is(notNullValue()));
    assertThat(output.getErrors().isEmpty(), is(true));
  }

  @Test
  public void testValidateAllMeasureGroupReturnTypesReturnsMeasureWithErrorsRemoved() throws Exception {
    Measure measure = Measure.builder().elmJson("{}")
        .error(MeasureErrorType.MISMATCH_CQL_POPULATION_RETURN_TYPES)
        .groups(List.of(
            Group.builder().id("Group1").populations(null).build(),
            Group.builder().id("Group2").populations(List.of(
                Population.builder().definition("").build(),
                Population.builder().definition("GOOD DEFINE HERE").build()
            )).build()
        ))
        .build();
    doNothing()
        .when(cqlDefinitionReturnTypeValidator).validateCqlDefinitionReturnTypes(any(Group.class), anyString());
    doNothing()
        .when(cqlObservationFunctionValidator).validateObservationFunctions(any(Group.class), anyString());

    Measure output = measureUtil.validateAllMeasureGroupReturnTypes(measure);
    assertThat(output, is(notNullValue()));
    assertThat(output.getErrors(), is(notNullValue()));
    assertThat(output.getErrors().isEmpty(), is(true));
  }

  @Test
  public void testValidateAllMeasureGroupReturnTypesReturnsMeasureWithErrorAddedForPopulation() throws Exception {
    Measure measure = Measure.builder().elmJson("{}")
        .groups(List.of(
            Group.builder().id("Group1").populations(null).build(),
            Group.builder().id("Group2").populations(List.of(
                Population.builder().definition("").build(),
                Population.builder().definition("GOOD DEFINE HERE").build()
            )).build()
        ))
        .build();
    doThrow(new InvalidReturnTypeException("DEFINITIONS"))
        .when(cqlDefinitionReturnTypeValidator).validateCqlDefinitionReturnTypes(any(Group.class), anyString());

    Measure output = measureUtil.validateAllMeasureGroupReturnTypes(measure);
    assertThat(output, is(notNullValue()));
    assertThat(output.getErrors(), is(notNullValue()));
    assertThat(output.getErrors().contains(MeasureErrorType.MISMATCH_CQL_POPULATION_RETURN_TYPES), is(true));
  }

  @Test
  public void testValidateAllMeasureGroupReturnTypesReturnsMeasureWithErrorAddedForObservation() throws Exception {
    Measure measure = Measure.builder().elmJson("{}")
        .groups(List.of(
            Group.builder().id("Group1").populations(null).build(),
            Group.builder().id("Group2").populations(List.of(
                Population.builder().definition("").build(),
                Population.builder().definition("GOOD DEFINE HERE").build()
            )).build()
        ))
        .build();
    doNothing()
        .when(cqlDefinitionReturnTypeValidator).validateCqlDefinitionReturnTypes(any(Group.class), anyString());
    doThrow(new InvalidReturnTypeException("OBSERVATIONS"))
        .when(cqlObservationFunctionValidator).validateObservationFunctions(any(Group.class), anyString());

    Measure output = measureUtil.validateAllMeasureGroupReturnTypes(measure);
    assertThat(output, is(notNullValue()));
    assertThat(output.getErrors(), is(notNullValue()));
    assertThat(output.getErrors().contains(MeasureErrorType.MISMATCH_CQL_POPULATION_RETURN_TYPES), is(true));
  }

  @Test
  public void testIsGroupsExistWithPopulationsReturnsFalseForNullMeasure() {
    boolean output = measureUtil.isGroupsExistWithPopulations(null);
    assertThat(output, is(false));
  }

  @Test
  public void testIsGroupsExistWithPopulationsReturnsFalseForNullGroups() {
    Measure measure = new Measure();
    measure.setGroups(null);
    boolean output = measureUtil.isGroupsExistWithPopulations(measure);
    assertThat(output, is(false));
  }

  @Test
  public void testIsGroupsExistWithPopulationsReturnsFalseForEmptyGroups() {
    Measure measure = Measure.builder().groups(List.of()).build();
    boolean output = measureUtil.isGroupsExistWithPopulations(measure);
    assertThat(output, is(false));
  }

  @Test
  public void testIsGroupsExistWithPopulationsReturnsFalseForGroupsWithNoPopulations() {
    Measure measure = Measure.builder()
        .groups(List.of(
            Group.builder().id("Group1").populations(null).build(),
            Group.builder().id("Group2").populations(null).build()
        ))
        .build();
    boolean output = measureUtil.isGroupsExistWithPopulations(measure);
    assertThat(output, is(false));
  }

  @Test
  public void testIsGroupsExistWithPopulationsReturnsFalseForGroupsWithPopulationsButAllNullDefines() {
    Measure measure = Measure.builder()
        .groups(List.of(
            Group.builder().id("Group1").populations(null).build(),
            Group.builder().id("Group2").populations(List.of(
                Population.builder().definition(null).build(),
                Population.builder().definition(null).build()
            )).build()
        ))
        .build();
    boolean output = measureUtil.isGroupsExistWithPopulations(measure);
    assertThat(output, is(false));
  }

  @Test
  public void testIsGroupsExistWithPopulationsReturnsFalseForGroupsWithPopulationsButAllBlankDefines() {
    Measure measure = Measure.builder()
        .groups(List.of(
            Group.builder().id("Group1").populations(null).build(),
            Group.builder().id("Group2").populations(List.of(
                Population.builder().definition("").build(),
                Population.builder().definition("").build()
            )).build()
        ))
        .build();
    boolean output = measureUtil.isGroupsExistWithPopulations(measure);
    assertThat(output, is(false));
  }

  @Test
  public void testIsGroupsExistWithPopulationsReturnsTrue() {
    Measure measure = Measure.builder()
        .groups(List.of(
            Group.builder().id("Group1").populations(null).build(),
            Group.builder().id("Group2").populations(List.of(
                Population.builder().definition("").build(),
                Population.builder().definition("DEFINE HERE").build()
            )).build()
        ))
        .build();
    boolean output = measureUtil.isGroupsExistWithPopulations(measure);
    assertThat(output, is(true));
  }

  @Test
  public void testIsGroupReturnTypesValidReturnsTrue() throws JsonProcessingException {
    doNothing()
        .when(cqlDefinitionReturnTypeValidator).validateCqlDefinitionReturnTypes(any(Group.class), anyString());
    doNothing()
        .when(cqlObservationFunctionValidator).validateObservationFunctions(any(Group.class), anyString());

    boolean output = measureUtil.isGroupReturnTypesValid(Group.builder().build(), "");
    assertThat(output, is(true));
  }

  @Test
  public void testIsGroupReturnTypesValidReturnsFalseForCqlDefinitionReturnTypesException() throws JsonProcessingException {
    doThrow(new InvalidReturnTypeException("DEFINITIONS"))
        .when(cqlDefinitionReturnTypeValidator).validateCqlDefinitionReturnTypes(any(Group.class), anyString());

    boolean output = measureUtil.isGroupReturnTypesValid(Group.builder().build(), "");
    assertThat(output, is(false));
    verify(cqlDefinitionReturnTypeValidator, times(1))
        .validateCqlDefinitionReturnTypes(any(Group.class), anyString());
    verifyNoInteractions(cqlObservationFunctionValidator);
  }

  @Test
  public void testIsGroupReturnTypesValidReturnsFalseForObservationsReturnTypesException() throws JsonProcessingException {
    doNothing()
        .when(cqlDefinitionReturnTypeValidator).validateCqlDefinitionReturnTypes(any(Group.class), anyString());
    doThrow(new InvalidReturnTypeException("OBSERVATIONS"))
        .when(cqlObservationFunctionValidator).validateObservationFunctions(any(Group.class), anyString());

    boolean output = measureUtil.isGroupReturnTypesValid(Group.builder().build(), "");
    assertThat(output, is(false));
    verify(cqlDefinitionReturnTypeValidator, times(1))
        .validateCqlDefinitionReturnTypes(any(Group.class), anyString());
    verify(cqlObservationFunctionValidator, times(1))
        .validateObservationFunctions(any(Group.class), anyString());
  }

  @Test
  public void testIsMeasureCqlChangedReturnsTrueForNullOriginalMeasure() {
    final Measure changed = Measure.builder().cql("this is the changed cql").build();
    boolean output = measureUtil.isMeasureCqlChanged(null, changed);
    assertThat(output, is(true));
  }

  @Test
  public void testIsMeasureCqlChangedReturnsTrueForNullChangedMeasure() {
    final Measure original = Measure.builder().cql("THIS IS THE ORIGINAL CQL").build();
    boolean output = measureUtil.isMeasureCqlChanged(original, null);
    assertThat(output, is(true));
  }

  @Test
  public void testIsMeasureCqlChangedReturnsTrueForNullOriginalCql() {
    final Measure original = Measure.builder().cql(null).build();
    final Measure changed = Measure.builder().cql("this is the changed cql").build();
    boolean output = measureUtil.isMeasureCqlChanged(original, changed);
    assertThat(output, is(true));
  }

  @Test
  public void testIsMeasureCqlChangedReturnsTrueForNullChangedCql() {
    final Measure original = Measure.builder().cql("THIS IS THE ORIGINAL CQL").build();
    final Measure changed = Measure.builder().cql(null).build();
    boolean output = measureUtil.isMeasureCqlChanged(original, changed);
    assertThat(output, is(true));
  }

  @Test
  public void testIsMeasureCqlChangedReturnsTrueForModifiedCql() {
    final Measure original = Measure.builder().cql("THIS IS THE ORIGINAL CQL").build();
    final Measure changed = Measure.builder().cql("this is the changed cql").build();
    boolean output = measureUtil.isMeasureCqlChanged(original, changed);
    assertThat(output, is(true));
  }
}
