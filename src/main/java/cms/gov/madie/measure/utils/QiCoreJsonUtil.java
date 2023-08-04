package cms.gov.madie.measure.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Iterator;
import java.util.UUID;

@Slf4j
public class QiCoreJsonUtil {

    public static String getPatientId(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        JsonNode jsonNode = mapper.readTree(json);
        JsonNode entry = jsonNode.get("entry");
        JsonNode theNode = null;
        Iterator<JsonNode> entyIter = entry.iterator();
        String existingPatientId = null;
        while (entyIter.hasNext()) {
            theNode = entyIter.next();
            var resourceNode = theNode.get("resource");
            var resourceType = resourceNode.get("resourceType");
            if ("PATIENT".equals(resourceType.asText().toUpperCase())) {
                existingPatientId = resourceNode.get("id").textValue();
            }
        }
        return existingPatientId;
    }

    public static String updateFullUrl(final String fullUrl, final String oldPatientId, final String newPatientId) {
        if (!StringUtils.isBlank(fullUrl) && fullUrl.endsWith(oldPatientId)) {
            return fullUrl.substring(0, fullUrl.length()-oldPatientId.length()) + newPatientId;
        }
        return fullUrl;
    }


    /**
     * Items to account for:
     *   1. Update ID on patient resource
     *   2. Update full URL on patient resource if the ID matches
     *   3. Update references on any other resources that point to patient resource
     * @param json
     * @param newPatientId
     * @return
     * @throws JsonProcessingException
     */
    public static String replacePatientId(String json, String newPatientId) throws JsonProcessingException {
        if (StringUtils.isBlank(json)) {
            return json;
        }

        ObjectMapper mapper = new ObjectMapper();
        var oldPatientId = getPatientId(json);
        if (NumberUtils.isCreatable(oldPatientId)) {

        }

        JsonNode rootNode = mapper.readTree(json);
        ArrayNode allEntries = (ArrayNode) rootNode.get("entry");
        if (allEntries != null) {
            for (JsonNode node : allEntries) {
                if (node.get("resource") != null
                        && node.get("resource").get("resourceType") != null
                        && node.get("resource").get("resourceType").asText().equalsIgnoreCase("Patient")) {
                    JsonNode resourceNode = node.get("resource");

                    ObjectNode parent = (ObjectNode) node;
                    if (parent.get("fullUrl") != null && !StringUtils.isBlank(parent.get("fullUrl").asText())) {
                        final String fullUrl = parent.get("fullUrl").asText();

                    }

                    ObjectNode o = (ObjectNode) resourceNode;

                    o.put("id", newPatientId);

                }
            }
        }

        return json;
    }

    public static String replacePatientRefs(String json, String oldPatientId, String newPatientId) {
        final String pattern = "(\"reference\"\\s*:\\s*\"Patient/"+oldPatientId+"\")";
        return json.replaceAll(pattern, "\"reference\": \"Patient/"+newPatientId+"\"");
    }

    public static String replacePatientRefs(String json, String newPatientId) {
        final String pattern = "(\"reference\"\\s*:\\s*\"Patient\\/[A-Za-z0-9\\-\\.]{1,64}\")";
        return json.replaceAll(pattern, "\"reference\": \"Patient/"+newPatientId+"\"");
    }

    public static boolean isUuid(String value) {
        try{
            UUID uuid = UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException exception){
            //handle the case where string is not valid UUID
        }
        return false;
    }
}
