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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class QiCoreJsonUtilTest {

  private TestCase testCase =
      TestCase.builder().patientId(UUID.fromString("3d2abb9d-c10a-4ab3-ae1a-1684ab61c07e")).build();
  final String json =
      "{\"resourceType\":\"Bundle\",\"id\":\"2106\",\"meta\":{\"versionId\":\"1\",\"lastUpdated\":\"2022-09-06T20:47:21.183+00:00\"},\"type\":\"collection\",\"entry\":[{\"fullUrl\":\"http://local/Encounter/2\",\"resource\":{\"id\":\"2\",\"resourceType\":\"Encounter\",\"meta\":{\"versionId\":\"1\",\"lastUpdated\":\"2021-10-13T03:34:10.160+00:00\",\"source\":\"#nEcAkGd8PRwPP5fA\"},\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">Sep 9th 2021 for Asthma<a name=\\\"mm\\\"/></div>\"},\"class\":{\"system\":\"http://terminology.hl7.org/CodeSystem/v3-ActCode\",\"code\":\"IMP\",\"display\":\"inpatient encounter\"},\"status\":\"planned\",\"type\":[{\"text\":\"OutPatient\"}],\"subject\":{\"reference\":\"Patient/1\"},\"participant\":[{\"individual\":{\"reference\":\"Practitioner/30164\",\"display\":\"Dr John Doe\"}}],\"period\":{\"start\":\"2023-08-10T03:34:10.054Z\",\"end\":\"2023-08-15T03:34:10.054Z\"}}},{\"fullUrl\":\"http://local/Encounter/3\",\"resource\":{\"id\":\"3\",\"resourceType\":\"Encounter\",\"meta\":{\"versionId\":\"1\",\"lastUpdated\":\"2021-10-13T03:34:10.160+00:00\",\"source\":\"#nEcAkGd8PRwPP5fA\"},\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">Sep 9th 2021 for Asthma<a name=\\\"mm\\\"/></div>\"},\"class\":{\"system\":\"http://terminology.hl7.org/CodeSystem/v3-ActCode\",\"code\":\"IMP\",\"display\":\"inpatient encounter\"},\"status\":\"finished\",\"type\":[{\"text\":\"OutPatient\"}],\"subject\":{\"reference\":\"Patient/1\"},\"participant\":[{\"individual\":{\"reference\":\"Practitioner/30164\",\"display\":\"Dr John Doe\"}}],\"period\":{\"start\":\"2023-09-12T03:34:10.054Z\",\"end\":\"2023-09-13T09:34:10.054Z\"}}},{\"fullUrl\":\"http://local/Patient/1\",\"resource\":{\"id\":\"1\",\"resourceType\":\"Patient\",\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">Lizzy Health</div>\"},\"meta\":{\"profile\":\"http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-patient\"},\"identifier\":[{\"system\":\"http://clinfhir.com/fhir/NamingSystem/identifier\",\"value\":\"20181011LizzyHealth\"}],\"name\":[{\"use\":\"official\",\"text\":\"Lizzy Health\",\"family\":\"Health\",\"given\":[\"Lizzy\"]}],\"gender\":\"female\",\"birthDate\":\"2000-10-11\"}}]}";
  final String json2 =
      "{ \"resourceType\": \"Bundle\", \"id\": \"NUMERFail-SBP139DBP89TakenDuringEMER\", \"type\": \"collection\", \"entry\": [ { \"fullUrl\": \"633c9d020968f8012250fc60\", \"resource\": { \"resourceType\": \"Patient\", \"id\": \"Patient-7\", \"meta\": { \"profile\": [ \"http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-patient\" ] }, \"extension\": [ { \"url\": \"http://hl7.org/fhir/us/core/StructureDefinition/us-core-race\", \"extension\": [ { \"url\": \"ombCategory\", \"valueCoding\": { \"code\": \"2028-9\", \"system\": \"urn:oid:2.16.840.1.113883.6.238\", \"display\": \"Asian\" } }, { \"url\": \"text\", \"valueString\": \"Asian\" } ] }, { \"url\": \"http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity\", \"extension\": [ { \"url\": \"ombCategory\", \"valueCoding\": { \"code\": \"2135-2\", \"system\": \"urn:oid:2.16.840.1.113883.6.238\", \"display\": \"Hispanic or Latino\" } }, { \"url\": \"text\", \"valueString\": \"Hispanic or Latino\" } ] } ], \"identifier\": [ { \"system\": \"http://hospital.smarthealthit.org\", \"value\": \"999999995\" } ], \"name\": [ { \"family\": \"Bertha\", \"given\": [ \"Betty\" ] } ], \"birthDate\": \"2005-12-31\", \"gender\": \"female\" } }, { \"fullUrl\": \"633c9d020968f8012250fc61\", \"resource\": { \"resourceType\": \"Encounter\", \"id\": \"Encounter-7\", \"meta\": { \"profile\": [ \"http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-encounter\" ] }, \"subject\": { \"reference\": \"Patient/Patient-7\" }, \"status\": \"finished\", \"class\": { \"code\": \"AMB\", \"system\": \"http://terminology.hl7.org/CodeSystem/v3-ActCode\", \"display\": \"ambulatory\" }, \"type\": [ { \"coding\": [ { \"code\": \"3391000175108\", \"system\": \"http://snomed.info/sct\", \"display\": \"Office visit for pediatric care and assessment (procedure)\" } ] } ], \"period\": { \"start\": \"2024-01-01T00:00:00.000Z\", \"end\": \"2024-01-01T01:00:00.000Z\" } } }, { \"fullUrl\": \"633c9d020968f8012250fc62\", \"resource\": { \"resourceType\": \"Condition\", \"id\": \"Condition-7\", \"meta\": { \"profile\": [ \"http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-condition\" ] }, \"category\": [ { \"coding\": [ { \"code\": \"problem-list-item\", \"system\": \"http://terminology.hl7.org/CodeSystem/condition-category\", \"display\": \"Problem List Item\" } ] } ], \"subject\": { \"reference\": \"Patient/Patient-7\" }, \"code\": { \"coding\": [ { \"code\": \"371125006\", \"system\": \"http://snomed.info/sct\", \"display\": \"Labile essential hypertension (disorder)\" } ] }, \"onsetDateTime\": \"2024-06-30T23:59:59.000Z\", \"clinicalStatus\": { \"coding\": [ { \"code\": \"active\", \"system\": \"http://terminology.hl7.org/CodeSystem/condition-clinical\" } ] } } }, { \"fullUrl\": \"633c9d020968f8012250fc63\", \"resource\": { \"resourceType\": \"Observation\", \"id\": \"Observation-7\", \"meta\": { \"profile\": [ \"http://hl7.org/fhir/StructureDefinition/bp\" ] }, \"subject\": { \"reference\": \"Patient/Patient-7\" }, \"category\": [ { \"coding\": [ { \"code\": \"vital-signs\", \"system\": \"http://terminology.hl7.org/CodeSystem/observation-category\", \"display\": \"Vital Signs\" } ], \"text\": \"Vital Signs\" } ], \"code\": { \"coding\": [ { \"code\": \"85354-9\", \"system\": \"http://loinc.org\", \"display\": \"Blood pressure panel with all children optional\" } ] }, \"component\": [ { \"code\": { \"coding\": [ { \"code\": \"8480-6\", \"system\": \"http://loinc.org\", \"display\": \"Systolic blood pressure\" } ] }, \"valueQuantity\": { \"value\": 139, \"code\": \"mm[Hg]\", \"system\": \"http://unitsofmeasure.org\", \"unit\": \"mmHg\" } }, { \"code\": { \"coding\": [ { \"code\": \"8462-4\", \"system\": \"http://loinc.org\", \"display\": \"Diastolic blood pressure\" } ] }, \"valueQuantity\": { \"value\": 89, \"code\": \"mm[Hg]\", \"system\": \"http://unitsofmeasure.org\", \"unit\": \"mmHg\" } } ], \"status\": \"final\", \"effectiveDateTime\": \"2024-01-01T00:00:00.000Z\", \"encounter\": { \"reference\": \"Encounter/Encounter-7-1\" } } }, { \"fullUrl\": \"633c9d020968f8012250fc64\", \"resource\": { \"resourceType\": \"Encounter\", \"id\": \"Encounter-7-1\", \"meta\": { \"profile\": [ \"http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-encounter\" ] }, \"subject\": { \"reference\": \"Patient/Patient-7\" }, \"status\": \"finished\", \"class\": { \"code\": \"EMER\", \"system\": \"http://terminology.hl7.org/CodeSystem/v3-ActCode\", \"display\": \"emergency\" }, \"type\": [ { \"coding\": [ { \"code\": \"4525004\", \"system\": \"http://snomed.info/sct\", \"display\": \"Emergency department patient visit (procedure)\" } ] } ], \"period\": { \"start\": \"2024-01-01T00:00:00.000Z\", \"end\": \"2024-01-01T01:00:00.000Z\" } } } ] } ";
  final String json3 =
      "{ \"resourceType\": \"Bundle\", \"id\": \"NUMERFail-SBP139DBP89TakenDuringEMER\", \"type\": \"collection\", \"entry\": [ { \"fullUrl\": \"http://local/Patient/Patient-7\", \"resource\": { \"resourceType\": \"Patient\", \"id\": \"Patient-7\", \"meta\": { \"profile\": [ \"http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-patient\" ] }, \"extension\": [ { \"url\": \"http://hl7.org/fhir/us/core/StructureDefinition/us-core-race\", \"extension\": [ { \"url\": \"ombCategory\", \"valueCoding\": { \"code\": \"2028-9\", \"system\": \"urn:oid:2.16.840.1.113883.6.238\", \"display\": \"Asian\" } }, { \"url\": \"text\", \"valueString\": \"Asian\" } ] }, { \"url\": \"http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity\", \"extension\": [ { \"url\": \"ombCategory\", \"valueCoding\": { \"code\": \"2135-2\", \"system\": \"urn:oid:2.16.840.1.113883.6.238\", \"display\": \"Hispanic or Latino\" } }, { \"url\": \"text\", \"valueString\": \"Hispanic or Latino\" } ] } ], \"identifier\": [ { \"system\": \"http://hospital.smarthealthit.org\", \"value\": \"999999995\" } ], \"name\": [ { \"family\": \"Bertha\", \"given\": [ \"Betty\" ] } ], \"birthDate\": \"2005-12-31\", \"gender\": \"female\" } }, { \"fullUrl\": \"633c9d020968f8012250fc61\", \"resource\": { \"resourceType\": \"Encounter\", \"id\": \"Encounter-7\", \"meta\": { \"profile\": [ \"http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-encounter\" ] }, \"subject\": { \"reference\": \"http://local/Patient/Patient-7\" }, \"status\": \"finished\", \"class\": { \"code\": \"AMB\", \"system\": \"http://terminology.hl7.org/CodeSystem/v3-ActCode\", \"display\": \"ambulatory\" }, \"type\": [ { \"coding\": [ { \"code\": \"3391000175108\", \"system\": \"http://snomed.info/sct\", \"display\": \"Office visit for pediatric care and assessment (procedure)\" } ] } ], \"period\": { \"start\": \"2024-01-01T00:00:00.000Z\", \"end\": \"2024-01-01T01:00:00.000Z\" } } }, { \"fullUrl\": \"633c9d020968f8012250fc62\", \"resource\": { \"resourceType\": \"Condition\", \"id\": \"Condition-7\", \"meta\": { \"profile\": [ \"http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-condition\" ] }, \"category\": [ { \"coding\": [ { \"code\": \"problem-list-item\", \"system\": \"http://terminology.hl7.org/CodeSystem/condition-category\", \"display\": \"Problem List Item\" } ] } ], \"subject\": { \"reference\": \"http://local/Patient/Patient-7\" }, \"code\": { \"coding\": [ { \"code\": \"371125006\", \"system\": \"http://snomed.info/sct\", \"display\": \"Labile essential hypertension (disorder)\" } ] }, \"onsetDateTime\": \"2024-06-30T23:59:59.000Z\", \"clinicalStatus\": { \"coding\": [ { \"code\": \"active\", \"system\": \"http://terminology.hl7.org/CodeSystem/condition-clinical\" } ] } } }, { \"fullUrl\": \"633c9d020968f8012250fc63\", \"resource\": { \"resourceType\": \"Observation\", \"id\": \"Observation-7\", \"meta\": { \"profile\": [ \"http://hl7.org/fhir/StructureDefinition/bp\" ] }, \"subject\": { \"reference\": \"http://local/Patient/Patient-7\" }, \"category\": [ { \"coding\": [ { \"code\": \"vital-signs\", \"system\": \"http://terminology.hl7.org/CodeSystem/observation-category\", \"display\": \"Vital Signs\" } ], \"text\": \"Vital Signs\" } ], \"code\": { \"coding\": [ { \"code\": \"85354-9\", \"system\": \"http://loinc.org\", \"display\": \"Blood pressure panel with all children optional\" } ] }, \"component\": [ { \"code\": { \"coding\": [ { \"code\": \"8480-6\", \"system\": \"http://loinc.org\", \"display\": \"Systolic blood pressure\" } ] }, \"valueQuantity\": { \"value\": 139, \"code\": \"mm[Hg]\", \"system\": \"http://unitsofmeasure.org\", \"unit\": \"mmHg\" } }, { \"code\": { \"coding\": [ { \"code\": \"8462-4\", \"system\": \"http://loinc.org\", \"display\": \"Diastolic blood pressure\" } ] }, \"valueQuantity\": { \"value\": 89, \"code\": \"mm[Hg]\", \"system\": \"http://unitsofmeasure.org\", \"unit\": \"mmHg\" } } ], \"status\": \"final\", \"effectiveDateTime\": \"2024-01-01T00:00:00.000Z\", \"encounter\": { \"reference\": \"Encounter/Encounter-7-1\" } } }, { \"fullUrl\": \"633c9d020968f8012250fc64\", \"resource\": { \"resourceType\": \"Encounter\", \"id\": \"Encounter-7-1\", \"meta\": { \"profile\": [ \"http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-encounter\" ] }, \"subject\": { \"reference\": \"http://local/Patient/Patient-7\" }, \"status\": \"finished\", \"class\": { \"code\": \"EMER\", \"system\": \"http://terminology.hl7.org/CodeSystem/v3-ActCode\", \"display\": \"emergency\" }, \"type\": [ { \"coding\": [ { \"code\": \"4525004\", \"system\": \"http://snomed.info/sct\", \"display\": \"Emergency department patient visit (procedure)\" } ] } ], \"period\": { \"start\": \"2024-01-01T00:00:00.000Z\", \"end\": \"2024-01-01T01:00:00.000Z\" } } } ] } ";
  final String malformedJson =
      "{ \"resourceType\": \"Bundle\", \"type\": \"collection\", \"entry\": [{ \"fullUrl\": \"633c9d020968f8012250fc60 }]}"; // intentional - missing quotes around fullUrl ID

  final String measureReportJson =
      "{\n"
          + "   \"resourceType\":\"Bundle\",\n"
          + "   \"id\":\"62c880eb0111a60120dc21eb\",\n"
          + "   \"type\":\"collection\",\n"
          + "   \"entry\":[\n"
          + "      {\n"
          + "         \"fullUrl\":\"https://madie.cms.gov/MeasureReport/642c4793b0dff949a62decc7/b7398102-3447-45b5-a082-e08fbe528396\",\n"
          + "         \"resource\":{\n"
          + "            \"resourceType\":\"MeasureReport\",\n"
          + "            \"id\":\"91cf317a-3da5-4797-b676-de3098217bd4\",\n"
          + "            \"group\":[\n"
          + "               {\n"
          + "                  \"population\":[\n"
          + "                     {\n"
          + "                        \"code\":{\n"
          + "                           \"coding\":[\n"
          + "                              {\n"
          + "                                 \"system\":\"http://terminology.hl7.org/CodeSystem/measure-population\",\n"
          + "                                 \"code\":\"initial-population\",\n"
          + "                                 \"display\":\"Initial Population\"\n"
          + "                              }\n"
          + "                           ]\n"
          + "                        },\n"
          + "                        \"count\":1\n"
          + "                     },\n"
          + "                     {\n"
          + "                        \"code\":{\n"
          + "                           \"coding\":[\n"
          + "                              {\n"
          + "                                 \"system\":\"http://terminology.hl7.org/CodeSystem/measure-population\",\n"
          + "                                 \"code\":\"denominator\",\n"
          + "                                 \"display\":\"Denominator\"\n"
          + "                              }\n"
          + "                           ]\n"
          + "                        },\n"
          + "                        \"count\":2\n"
          + "                     },\n"
          + "                     {\n"
          + "                        \"code\":{\n"
          + "                           \"coding\":[\n"
          + "                              {\n"
          + "                                 \"system\":\"http://terminology.hl7.org/CodeSystem/measure-population\",\n"
          + "                                 \"code\":\"numerator\",\n"
          + "                                 \"display\":\"Numerator\"\n"
          + "                              }\n"
          + "                           ]\n"
          + "                        },\n"
          + "                        \"count\":3\n"
          + "                     }\n"
          + "                  ]\n"
          + "               },\n"
          + "               {\n"
          + "                  \"population\":[\n"
          + "                     {\n"
          + "                        \"code\":{\n"
          + "                           \"coding\":[\n"
          + "                              {\n"
          + "                                 \"system\":\"http://terminology.hl7.org/CodeSystem/measure-population\",\n"
          + "                                 \"code\":\"initial-population\",\n"
          + "                                 \"display\":\"Initial Population\"\n"
          + "                              }\n"
          + "                           ]\n"
          + "                        },\n"
          + "                        \"count\":4\n"
          + "                     },\n"
          + "                     {\n"
          + "                        \"code\":{\n"
          + "                           \"coding\":[\n"
          + "                              {\n"
          + "                                 \"system\":\"http://terminology.hl7.org/CodeSystem/measure-population\",\n"
          + "                                 \"code\":\"denominator\",\n"
          + "                                 \"display\":\"Denominator\"\n"
          + "                              }\n"
          + "                           ]\n"
          + "                        },\n"
          + "                        \"count\":5\n"
          + "                     },\n"
          + "                     {\n"
          + "                        \"code\":{\n"
          + "                           \"coding\":[\n"
          + "                              {\n"
          + "                                 \"system\":\"http://terminology.hl7.org/CodeSystem/measure-population\",\n"
          + "                                 \"code\":\"numerator\",\n"
          + "                                 \"display\":\"Numerator\"\n"
          + "                              }\n"
          + "                           ]\n"
          + "                        },\n"
          + "                        \"count\":6\n"
          + "                     }\n"
          + "                  ]\n"
          + "               }\n"
          + "            ]\n"
          + "         }\n"
          + "      }\n"
          + "   ]\n"
          + "}";
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
    assertThat(json3.contains("\"reference\": \"http://local/Patient/Patient-7\""), is(true));
    String output =
        QiCoreJsonUtil.replaceFullUrlRefs(
            json3, "http://local/Patient/Patient-7", "a64561f9-5654-4e45-ac06-1c168f411345");
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
    String modifiedJson = QiCoreJsonUtil.enforcePatientId(testCase);
    assertNull(modifiedJson);
  }

  @Test
  public void testEnforcePatientIdNoEntry() {
    String json = "{\"resourceType\": \"Bundle\", \"type\": \"collection\"}";
    testCase.setJson(json);
    String modifiedJson = QiCoreJsonUtil.enforcePatientId(testCase);
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
    String modifiedJson = QiCoreJsonUtil.enforcePatientId(testCase);
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
    String modifiedJson = QiCoreJsonUtil.enforcePatientId(testCase);
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
    String modifiedJson = QiCoreJsonUtil.enforcePatientId(testCase);
    assertEquals(modifiedJson, json);
  }

  @Test
  public void testGetByteArrayOutputStreamThrowsException() {
    ByteArrayOutputStream bout = QiCoreJsonUtil.getByteArrayOutputStream(null, null);
    assertTrue(StringUtils.isAllBlank(bout.toString()));
  }

  @Test
  public void testRemoveMeasureReportFromJsonThrowsException() {
    assertThrows(RuntimeException.class, () -> QiCoreJsonUtil.removeMeasureReportFromJson(null));
  }
}
