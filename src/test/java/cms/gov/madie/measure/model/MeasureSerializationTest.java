package cms.gov.madie.measure.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cms.gov.madie.measure.models.Measure;
import cms.gov.madie.measure.models.MeasureScoring;

class MeasureSerializationTest {

  @Test
  void test() {
    ObjectMapper objectMapper = new ObjectMapper();

    String measureString =
        "{\n"
            + "    \"id\": \"61cb874f39eb8808217b66bc\",\n"
            + "    \"measureHumanReadableId\": null,\n"
            + "    \"measureSetId\": \"IDIDIDIDID\",\n"
            + "    \"version\": null,\n"
            + "    \"revisionNumber\": null,\n"
            + "    \"state\": null,\n"
            + "    \"cqlLibraryName\": \"MeasureSix\",\n"
            + "    \"measureName\": \"Measure Six\",\n"
            + "    \"cql\": null,\n"
            + "    \"createdAt\": null,\n"
            + "    \"createdBy\": null,\n"
            + "    \"lastModifiedAt\": null,\n"
            + "    \"lastModifiedBy\": null,\n"
            + "    \"model\": null,\n"
            + "    \"measureScoring\": \"Cohort\",\n"
            + "    \"measureMetaData\": {\n"
            + "        \"steward\": \"Bill\",\n \"description\": \"TestDescription\"\n"
            + "    }\n"
            + "}";

    Measure measure;
    try {
      measure = objectMapper.readValue(measureString, Measure.class);
      assertEquals(measure.getMeasureScoring(), MeasureScoring.COHORT.toString());
    } catch (JsonProcessingException e) {
      fail(e);
    }
  }
}
