package cms.gov.madie.measure.utils;

import com.fasterxml.jackson.core.JsonProcessingException;

import gov.cms.madie.models.measure.TestCase;
import gov.cms.madie.models.measure.TestCaseGroupPopulation;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class QiCoreJsonUtilTest implements ResourceUtil {

  private String baseUrl = "https://myorg.com";
  private TestCase testCase =
      TestCase.builder().patientId(UUID.fromString("3d2abb9d-c10a-4ab3-ae1a-1684ab61c07e")).build();
  final String json = getData("/bundles/qicore_json_util_testjson1.json");
  final String json2 = getData("/bundles/qicore_json_util_testjson2.json");
  final String malformedJson =
      "{ \"resourceType\": \"Bundle\", \"type\": \"collection\", \"entry\": [{ \"fullUrl\": \"633c9d020968f8012250fc60 }]}"; // intentional - missing quotes around fullUrl ID
  final String measureReportJson = getData("/bundles/qicore_json_util_measurereport.json");
  final String json_noEntries =
      "{\n"
          + "   \"resourceType\":\"Bundle\",\n"
          + "   \"id\":\"62c880eb0111a60120dc21eb\",\n"
          + "   \"type\":\"collection\"\n"
          + "}";
  final String json_noResource =
      "{\n"
          + "  \"id\": \"62c880eb0111a60120dc21eb\",\n"
          + "  \"type\": \"collection\",\n"
          + "  \"entry\": [ {\n"
          + "    \"fullUrl\": \"62c880eb0111a60120dc21eb\"\n"
          + "  }]\n"
          + "}";
  final String json_noResourceType =
      "{\n"
          + "  \"id\": \"62c880eb0111a60120dc21eb\",\n"
          + "  \"entry\": [ {\n"
          + "    \"resource\": {\n"
          + "      \"id\": \"62c880eb0111a60120dc21eb\"\n"
          + "    }\n"
          + "  }]\n"
          + "}";
  final String json_noName =
      "{\n"
          + "  \"id\": \"62c880eb0111a60120dc21eb\",\n"
          + "  \"entry\": [ {\n"
          + "    \"resource\": {\n"
          + "      \"resourceType\": \"Patient\",\n"
          + "      \"id\": \"62c880eb0111a60120dc21eb\"\n"
          + "    }\n"
          + "  }]\n"
          + "}";
  final String json_noGivenName =
      "{\n"
          + "  \"id\": \"62c880eb0111a60120dc21eb\",\n"
          + "  \"entry\": [ {\n"
          + "    \"resource\": {\n"
          + "      \"resourceType\": \"Patient\",\n"
          + "      \"id\": \"62c880eb0111a60120dc21eb\",\n"
          + "      \"name\": [ {\n"
          + "        \"family\": \"TestFamilyName\"\n"
          + "      } ]\n"
          + "    }\n"
          + "  }]\n"
          + "}";
  final String json_noGroup =
      "{\n"
          + "  \"id\": \"62c880eb0111a60120dc21eb\",\n"
          + "  \"entry\": [ {\n"
          + "    \"resource\": {\n"
          + "      \"resourceType\": \"MeasureReport\",\n"
          + "      \"id\": \"62c880eb0111a60120dc21eb\"\n"
          + "    }\n"
          + "  }]\n"
          + "}";
  final String json_noPopulation =
      "{\n"
          + "  \"id\": \"62c880eb0111a60120dc21eb\",\n"
          + "  \"entry\": [ {\n"
          + "    \"resource\": {\n"
          + "      \"resourceType\": \"MeasureReport\",\n"
          + "      \"id\": \"62c880eb0111a60120dc21eb\",\n"
          + "      \"group\": [ {\n"
          + "      } ]\n"
          + "    }\n"
          + "  }]\n"
          + "}";
  final String json_noCode =
      "{\n"
          + "  \"id\": \"62c880eb0111a60120dc21eb\",\n"
          + "  \"entry\": [ {\n"
          + "    \"resource\": {\n"
          + "      \"resourceType\": \"MeasureReport\",\n"
          + "      \"id\": \"62c880eb0111a60120dc21eb\",\n"
          + "      \"group\": [ {\n"
          + "          \"population\" : [{\n"
          + "          }]\n"
          + "      } ]\n"
          + "    }\n"
          + "  }]\n"
          + "}";
  final String json_noCount =
      "{\n"
          + "  \"id\": \"62c880eb0111a60120dc21eb\",\n"
          + "  \"entry\": [ {\n"
          + "    \"resource\": {\n"
          + "      \"resourceType\": \"MeasureReport\",\n"
          + "      \"id\": \"62c880eb0111a60120dc21eb\",\n"
          + "      \"group\": [ {\n"
          + "          \"population\" : [{\n"
          + "              \"code\" : {}\n"
          + "          }]\n"
          + "      } ]\n"
          + "    }\n"
          + "  }]\n"
          + "}";

  @Test
  public void testIsValidJsonSuccess() {
    boolean output = QiCoreJsonUtil.isValidJson(json);
    assertThat(output, is(true));
  }

  @Test
  public void testIsValidJsonFalse() {
    boolean output = QiCoreJsonUtil.isValidJson(malformedJson);
    assertThat(output, is(false));
  }

  @Test
  public void testGetPatientId() throws JsonProcessingException {
    String output = QiCoreJsonUtil.getPatientId(json);
    assertThat(output, is(equalTo("1")));
  }

  @Test
  public void testUpdateFullUrlNoChange() {
    final String fullUrl = "https://something/Patient/foo";
    final String output =
        QiCoreJsonUtil.updateFullUrl(fullUrl, "patient1", "a64561f9-5654-4e45-ac06-1c168f411345");
    assertThat(output, is(equalTo(fullUrl)));
  }

  @Test
  public void testUpdateFullUrlUpdatesSuccessfully() {
    final String fullUrl = "https://something/Patient/patient1";
    final String output =
        QiCoreJsonUtil.updateFullUrl(fullUrl, "patient1", "a64561f9-5654-4e45-ac06-1c168f411345");
    assertThat(
        output, is(equalTo("https://something/Patient/a64561f9-5654-4e45-ac06-1c168f411345")));
  }

  @Test
  public void testUpdateFullUrlUpdatesOnlyLastInstanceSuccessfully() {
    final String fullUrl = "https://something/patient1/patient1/Patient/patient1/patient1";
    final String output =
        QiCoreJsonUtil.updateFullUrl(fullUrl, "patient1", "a64561f9-5654-4e45-ac06-1c168f411345");
    assertThat(
        output,
        is(
            equalTo(
                "https://something/patient1/patient1/Patient/patient1/a64561f9-5654-4e45-ac06-1c168f411345")));
  }

  @Test
  public void testUpdateFullUrlUpdatesStringWithOnlyId() {
    final String fullUrl = "patient1";
    final String output =
        QiCoreJsonUtil.updateFullUrl(fullUrl, "patient1", "a64561f9-5654-4e45-ac06-1c168f411345");
    assertThat(output, is(equalTo("a64561f9-5654-4e45-ac06-1c168f411345")));
  }

  @Test
  public void testReplaceReferencesDoesNothing() {
    String output = QiCoreJsonUtil.replacePatientRefs(json, "FOO12344", "BillyBob");
    assertThat(output, is(equalTo(json)));
  }

  @Test
  public void testReplaceReference() {
    // make sure it's there to start with
    assertThat(json.contains("\"Patient/1\""), is(true));
    String output =
        QiCoreJsonUtil.replacePatientRefs(json, "1", "a64561f9-5654-4e45-ac06-1c168f411345");
    assertThat(output, is(not(equalTo(json))));
    assertThat(output.contains("\"Patient/1\""), is(false));
  }

  @Test
  public void testReplaceReferenceWithoutOldId() {
    // make sure it's there to start with
    assertThat(json.contains("\"Patient/1\""), is(true));
    String output = QiCoreJsonUtil.replacePatientRefs(json, "a64561f9-5654-4e45-ac06-1c168f411345");
    assertThat(output, is(not(equalTo(json))));
    assertThat(output.contains("\"Patient/1\""), is(false));
    assertThat(output.contains("\"Patient/a64561f9-5654-4e45-ac06-1c168f411345\""), is(true));
  }

  @Test
  public void testReplaceFullUrlRefsWorks() {
    String output =
        QiCoreJsonUtil.replaceFullUrlRefs(
            "{ \"reference\" : \"http://local/Patient/1\" }",
            "http://local/Patient/1",
            "a64561f9-5654-4e45-ac06-1c168f411345");
    assertThat(
        output, is(equalTo("{ \"reference\": \"Patient/a64561f9-5654-4e45-ac06-1c168f411345\" }")));
  }

  @Test
  public void testReplaceFullUrlRefsHandlesFullJson() {
    assertThat(json2.contains("reference\":\"http://local/Patient/Patient-7"), is(true));
    String output =
        QiCoreJsonUtil.replaceFullUrlRefs(
            json2, "http://local/Patient/Patient-7", "a64561f9-5654-4e45-ac06-1c168f411345");
    assertThat(output.contains("\"reference\": \"http://local/Patient/Patient-7\""), is(false));
    assertThat(
        output.contains("\"reference\": \"Patient/a64561f9-5654-4e45-ac06-1c168f411345\""),
        is(true));
  }

  @Test
  public void testIsUuiReturnsFalseForNull() {
    assertThat(QiCoreJsonUtil.isUuid(null), is(false));
  }

  @Test
  public void testIsUuiReturnsFalseForEmptyString() {
    assertThat(QiCoreJsonUtil.isUuid(""), is(false));
  }

  @Test
  public void testIsUuiReturnsFalseForObjectId() {
    assertThat(QiCoreJsonUtil.isUuid("63bc5891ee2e584d9c7d819b"), is(false));
  }

  @Test
  public void testIsUuiReturnsFalseForRandomString() {
    assertThat(QiCoreJsonUtil.isUuid("RandomStringHere"), is(false));
  }

  @Test
  public void testIsUuiReturnsFalseForAlmostUuid() {
    assertThat(QiCoreJsonUtil.isUuid("a500cba-353-050-9a7"), is(false));
  }

  @Test
  public void testIsUuiReturnsTrueForUuid() {
    assertThat(QiCoreJsonUtil.isUuid("a500ccba-a353-4050-94a7-50f4eac4e59f"), is(true));
  }

  @Test
  public void testGetPatientFamilyName() throws JsonProcessingException {
    String output = QiCoreJsonUtil.getPatientName(json, "family");
    assertThat(output, is(equalTo("Health")));
  }

  @Test
  public void testGetPatientGivenName() throws JsonProcessingException {
    String output = QiCoreJsonUtil.getPatientName(json, "given");
    assertThat(output, is(equalTo("Lizzy")));
  }

  @Test
  public void testGetPatientFamilyNameNoEntries() throws JsonProcessingException {
    String output = QiCoreJsonUtil.getPatientName(json_noEntries, "family");
    assertThat(output, is(equalTo(null)));
  }

  @Test
  public void testGetPatientFamilyNameNoResource() throws JsonProcessingException {
    String output = QiCoreJsonUtil.getPatientName(json_noResource, "family");
    assertThat(output, is(equalTo(null)));
  }

  @Test
  public void testGetPatientFamilyNameNoResourceType() throws JsonProcessingException {
    String output = QiCoreJsonUtil.getPatientName(json_noResourceType, "family");
    assertThat(output, is(equalTo(null)));
  }

  @Test
  public void testGetPatientFamilyNameWrongtype() throws JsonProcessingException {
    String output = QiCoreJsonUtil.getPatientName(json, "wrongType");
    assertThat(output, is(equalTo(null)));
  }

  @Test
  public void testGetPatientFamilyNameNoName() throws JsonProcessingException {
    String output = QiCoreJsonUtil.getPatientName(json_noName, "family");
    assertThat(output, is(equalTo(null)));
  }

  @Test
  public void testGetPatientFamilyNameNoGivenName() throws JsonProcessingException {
    String output = QiCoreJsonUtil.getPatientName(json_noGivenName, "given");
    assertThat(output, is(equalTo(null)));
  }

  @Test
  public void testGetTestCaseGroupPopulationsFromMeasureReport() throws JsonProcessingException {
    List<TestCaseGroupPopulation> testCaseGroupPopulations =
        QiCoreJsonUtil.getTestCaseGroupPopulationsFromMeasureReport(measureReportJson);
    assertThat(testCaseGroupPopulations.size(), is(equalTo(2)));
    log.debug("testCaseGroupPopulations size  = " + testCaseGroupPopulations.size());

    assertThat(
        testCaseGroupPopulations.get(0).getPopulationValues().get(0).getExpected(),
        is(equalTo("1")));
    assertThat(
        testCaseGroupPopulations.get(0).getPopulationValues().get(1).getExpected(),
        is(equalTo("2")));
    assertThat(
        testCaseGroupPopulations.get(0).getPopulationValues().get(2).getExpected(),
        is(equalTo("3")));

    assertThat(
        testCaseGroupPopulations.get(1).getPopulationValues().get(0).getExpected(),
        is(equalTo("4")));
    assertThat(
        testCaseGroupPopulations.get(1).getPopulationValues().get(1).getExpected(),
        is(equalTo("5")));
    assertThat(
        testCaseGroupPopulations.get(1).getPopulationValues().get(2).getExpected(),
        is(equalTo("6")));
  }

  @Test
  public void testGetTestCaseGroupPopulationsFromMeasureReportNoEntries()
      throws JsonProcessingException {
    List<TestCaseGroupPopulation> testCaseGroupPopulations =
        QiCoreJsonUtil.getTestCaseGroupPopulationsFromMeasureReport(json_noEntries);
    assertThat(testCaseGroupPopulations.size(), is(equalTo(0)));
  }

  @Test
  public void testGetTestCaseGroupPopulationsFromMeasureReportNoResource()
      throws JsonProcessingException {
    List<TestCaseGroupPopulation> testCaseGroupPopulations =
        QiCoreJsonUtil.getTestCaseGroupPopulationsFromMeasureReport(json_noResource);
    assertThat(testCaseGroupPopulations.size(), is(equalTo(0)));
  }

  @Test
  public void testGetTestCaseGroupPopulationsFromMeasureReportNoResourceType()
      throws JsonProcessingException {
    List<TestCaseGroupPopulation> testCaseGroupPopulations =
        QiCoreJsonUtil.getTestCaseGroupPopulationsFromMeasureReport(json_noResourceType);
    assertThat(testCaseGroupPopulations.size(), is(equalTo(0)));
  }

  @Test
  public void testGetTestCaseGroupPopulationsFromMeasureReportNoGroup()
      throws JsonProcessingException {
    List<TestCaseGroupPopulation> testCaseGroupPopulations =
        QiCoreJsonUtil.getTestCaseGroupPopulationsFromMeasureReport(json_noGroup);
    assertThat(testCaseGroupPopulations.size(), is(equalTo(0)));
  }

  @Test
  public void testGetTestCaseGroupPopulationsFromMeasureReportNoPopulation()
      throws JsonProcessingException {
    List<TestCaseGroupPopulation> testCaseGroupPopulations =
        QiCoreJsonUtil.getTestCaseGroupPopulationsFromMeasureReport(json_noPopulation);
    assertThat(testCaseGroupPopulations.size(), is(equalTo(0)));
  }

  @Test
  public void testGetTestCaseGroupPopulationsFromMeasureReportNoCode()
      throws JsonProcessingException {
    List<TestCaseGroupPopulation> testCaseGroupPopulations =
        QiCoreJsonUtil.getTestCaseGroupPopulationsFromMeasureReport(json_noCode);
    assertThat(testCaseGroupPopulations.size(), is(equalTo(0)));
  }

  @Test
  public void testGetTestCaseGroupPopulationsFromMeasureReportNoCount()
      throws JsonProcessingException {
    List<TestCaseGroupPopulation> testCaseGroupPopulations =
        QiCoreJsonUtil.getTestCaseGroupPopulationsFromMeasureReport(json_noCount);
    assertThat(testCaseGroupPopulations.size(), is(equalTo(0)));
  }

  @Test
  public void testEnforcePatientIdEmptyJson() {
    testCase.setJson(null);
    String modifiedJson = QiCoreJsonUtil.enforcePatientId(testCase, baseUrl);
    assertNull(modifiedJson);
  }

  @Test
  public void testEnforcePatientIdNoEntry() {
    String json = "{\"resourceType\": \"Bundle\", \"type\": \"collection\"}";
    testCase.setJson(json);
    String modifiedJson = QiCoreJsonUtil.enforcePatientId(testCase, baseUrl);
    assertEquals(modifiedJson, json);
  }

  @Test
  public void testEnforcePatientIdNoResource() {
    String json =
        "{\"resourceType\": \"Bundle\", \"type\": \"collection\", \n"
            + "  \"entry\" : [ {\n"
            + "    \"fullUrl\" : \"http://local/Patient/1\"\n"
            + "  } ]             }";
    testCase.setJson(json);
    String modifiedJson = QiCoreJsonUtil.enforcePatientId(testCase, baseUrl);
    assertEquals(modifiedJson, json);
  }

  @Test
  public void testEnforcePatientIdNoResourceType() {
    String json =
        "{\"resourceType\": \"Bundle\", \"type\": \"collection\", \n"
            + "  \"entry\" : [ {\n"
            + "    \"fullUrl\" : \"http://local/Patient/1\",\n"
            + "    \"resource\" : {\n"
            + "      \"id\" : \"testUniqueId\"\n"
            + "    }\n"
            + "  } ]             }";
    testCase.setJson(json);
    String modifiedJson = QiCoreJsonUtil.enforcePatientId(testCase, baseUrl);
    assertEquals(modifiedJson, json);
  }

  @Test
  public void testEnforcePatientIdNoPatientResourceType() {
    String json =
        "{\"resourceType\": \"Bundle\", \"type\": \"collection\", \n"
            + "  \"entry\" : [ {\n"
            + "    \"fullUrl\" : \"http://local/Patient/1\",\n"
            + "    \"resource\" : {\n"
            + "      \"id\" : \"testUniqueId\",\n"
            + "      \"resourceType\" : \"NOTPatient\"    \n"
            + "    }\n"
            + "  } ]             }";
    testCase.setJson(json);
    String modifiedJson = QiCoreJsonUtil.enforcePatientId(testCase, baseUrl);
    assertEquals(modifiedJson, json);
  }

  @Test
  public void testRemoveMeasureReportFromJsonThrowsException() {
    assertThrows(RuntimeException.class, () -> QiCoreJsonUtil.removeMeasureReportFromJson(null));
  }

  @Test
  public void testJsonNodeToString() {
    String str = QiCoreJsonUtil.jsonNodeToString(null, null);
    assertTrue(StringUtils.isAllBlank(str));
  }

  @Test
  void updateResourceFullUrlsIfTestResourcesAvailable() {
    final String json = getData("/bundles/qicore_json_util_fullurl.json");
    TestCase tc1 =
        TestCase.builder().id("TC1").name("TC1").patientId(UUID.randomUUID()).json(json).build();
    String updatedTc1 = QiCoreJsonUtil.updateResourceFullUrls(tc1, baseUrl);
    assertNotEquals(updatedTc1, json);
    assertTrue(updatedTc1.contains(baseUrl));
  }

  @Test
  void updateResourceFullUrlsIfNoTestResourceAvailable() {
    final String json =
        "{\"id\":\"6323489059967e30c06d0774\",\"resourceType\":\"Bundle\",\"type\":\"collection\",\"entry\":[]}";
    TestCase tc1 =
        TestCase.builder().id("TC1").name("TC1").patientId(UUID.randomUUID()).json(json).build();
    String baseUrl = "https://myorg.com";
    String updatedTc1 = QiCoreJsonUtil.updateResourceFullUrls(tc1, baseUrl);
    assertFalse(updatedTc1.contains(baseUrl));
  }

  @Test
  void updateResourceFullUrlsIfEntryNodeNotAvailable() {
    final String json =
        "{\"id\":\"6323489059967e30c06d0774\",\"resourceType\":\"Bundle\",\"type\":\"collection\"}";
    TestCase tc1 =
        TestCase.builder().id("TC1").name("TC1").patientId(UUID.randomUUID()).json(json).build();
    String baseUrl = "https://myorg.com";
    String updatedTc1 = QiCoreJsonUtil.updateResourceFullUrls(tc1, baseUrl);
    assertFalse(updatedTc1.contains(baseUrl));
  }
}
