package cms.gov.madie.measure.validations;

import cms.gov.madie.measure.exceptions.InvalidIdException;
import cms.gov.madie.measure.exceptions.InvalidReturnTypeException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.MeasureObservation;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class CqlFunctionReturnTypeValidator {

  public void validateCqlFunctionReturnTypes(Group group, String elmJson)
      throws JsonProcessingException {
    Map<String, String> cqlFunctionReturnTypes = getCqlFunctionReturnTypes(elmJson);
    if (cqlFunctionReturnTypes.isEmpty()) {
      throw new InvalidIdException("No elm json available");
    }
    List<MeasureObservation> observations = group.getMeasureObservations();
    String populationBasis = group.getPopulationBasis().replaceAll("\\s", "");
    if (observations != null) {
      observations.forEach(
          observation -> {
            if (StringUtils.isNotBlank(observation.getDefinition())) {
              String returnType = cqlFunctionReturnTypes.get(observation.getDefinition());
              if (!StringUtils.equalsIgnoreCase(returnType, populationBasis)) {
                if ("boolean".equalsIgnoreCase(populationBasis)) {
                  throw new InvalidReturnTypeException(
                      "Selected function '%s' can not have parameters",
                      observation.getDefinition());
                }
                throw new InvalidReturnTypeException(
                    "Selected function must have exactly one parameter of type '%s'",
                    populationBasis);
              }
            }
          });
    }
  }

  /**
   * This method generates the map of cql functions & their return types.
   *
   * @param elmJson
   * @return
   * @throws JsonProcessingException
   */
  private Map<String, String> getCqlFunctionReturnTypes(String elmJson)
      throws JsonProcessingException {
    Map<String, String> returnTypes = new HashMap<>();
    if (StringUtils.isEmpty(elmJson)) {
      return returnTypes;
    }

    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode rootNode = objectMapper.readTree(elmJson);
    ArrayNode allDefinitions = (ArrayNode) rootNode.get("library").get("statements").get("def");
    Iterator<JsonNode> nodeIterator = allDefinitions.iterator();
    while (nodeIterator.hasNext()) {
      JsonNode node = nodeIterator.next();
      if (node.get("type") != null && "FunctionDef".equals(node.get("type").asText())) {
        int numberOfOperands = node.get("operand").size();
        Iterator<JsonNode> operandIterator = node.get("operand").iterator();
        if (numberOfOperands > 0 && numberOfOperands < 2) {
          String operandTypeSpecifierName =
              operandIterator.next().get("operandTypeSpecifier").get("name").asText();
          returnTypes.put(node.get("name").asText(), operandTypeSpecifierName.split("}")[1]);
        } else if (numberOfOperands < 1) {
          returnTypes.put(node.get("name").asText(), "Boolean");
        } else {
          returnTypes.put(node.get("name").asText(), "NA");
        }
      }
    }
    return returnTypes;
  }
}
