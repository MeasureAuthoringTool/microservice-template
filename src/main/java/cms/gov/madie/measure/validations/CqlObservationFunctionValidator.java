package cms.gov.madie.measure.validations;

import cms.gov.madie.measure.exceptions.InvalidReturnTypeException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.MeasureObservation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CqlObservationFunctionValidator {

  public void validateObservationFunctions(Group group, String elmJson)
      throws JsonProcessingException {
    Map<String, String> observationsToValidPopBasis = mapObservationsToValidPopBasis(elmJson);
    if (observationsToValidPopBasis.isEmpty()) {
      return; // observations are optional for scoring types other than Continuous Variable.
    }

    String groupPopulationBasis = group.getPopulationBasis().replaceAll("\\s", "");
    List<MeasureObservation> observations = group.getMeasureObservations();
    if (observations != null) {
      observations.forEach(
          observation -> {
            if (StringUtils.isNotBlank(observation.getDefinition())) {
              String observationValidPopBasis =
                  observationsToValidPopBasis.get(observation.getDefinition());
              if (!StringUtils.equalsIgnoreCase(observationValidPopBasis, groupPopulationBasis)) {
                if ("boolean".equalsIgnoreCase(groupPopulationBasis)) {
                  throw new InvalidReturnTypeException(
                      "Selected observation function '%s' can not have parameters",
                      observation.getDefinition());
                }
                throw new InvalidReturnTypeException(
                    "Selected observation function must have exactly one parameter of type '%s'",
                    groupPopulationBasis);
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
  private Map<String, String> mapObservationsToValidPopBasis(String elmJson)
      throws JsonProcessingException {
    // Determine which Population Basis the MO would be valid against.
    Map<String, String> observationPopBasis = new HashMap<>();
    if (StringUtils.isEmpty(elmJson)) {
      return observationPopBasis;
    }

    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode rootNode = objectMapper.readTree(elmJson);
    ArrayNode allDefinitions = (ArrayNode) rootNode.get("library").get("statements").get("def");
    for (JsonNode node : allDefinitions) {
      if (node.get("type") != null && "FunctionDef".equals(node.get("type").asText())) {
        int numberOfOperands = node.get("operand").size();
        Iterator<JsonNode> operandIterator = node.get("operand").iterator();
        // Non-Boolean Population Basis require MO's with exactly one parameter matching the Pop
        // Basis.
        if (numberOfOperands == 1) {
          String operandTypeSpecifierName =
              operandIterator.next().get("operandTypeSpecifier").get("name").asText();
          if (!(operandTypeSpecifierName.split("}")[1].equalsIgnoreCase("Boolean"))) {
            observationPopBasis.put(
                node.get("name").asText(), operandTypeSpecifierName.split("}")[1]);
          }
          // Boolean Population Basis require MO's with no parameters
        } else if (numberOfOperands < 1) {
          observationPopBasis.put(node.get("name").asText(), "Boolean");
          // Not a valid MO against any Population Basis
        }
        observationPopBasis.putIfAbsent(node.get("name").asText(), "NA");
      }
    }
    return observationPopBasis;
  }
}
