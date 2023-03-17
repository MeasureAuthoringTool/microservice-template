package cms.gov.madie.measure.validations;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import cms.gov.madie.measure.exceptions.InvalidReturnTypeException;
import gov.cms.madie.models.measure.DefDescPair;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.Stratification;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CqlDefinitionReturnTypeValidator {

  /**
   * Validate cql definition return types of a group against population basis. Group can have cql
   * definitions for populations, stratifications and observations
   */
  public void validateCqlDefinitionReturnTypes(Group group, String elmJson)
      throws JsonProcessingException {
    Map<String, String> cqlDefinitionReturnTypes = getCqlDefinitionReturnTypes(elmJson);
    if (cqlDefinitionReturnTypes.isEmpty()) {
      throw new IllegalArgumentException("No definitions found.");
    }

    List<Population> populations = group.getPopulations();
    String populationBasis = group.getPopulationBasis().replaceAll("\\s", "");
    if (populations != null) {
      populations.forEach(
          population -> {
            if (StringUtils.isNotBlank(population.getDefinition())) {
              String returnType = cqlDefinitionReturnTypes.get(population.getDefinition());
              if (!StringUtils.equalsIgnoreCase(returnType, populationBasis)) {
                throw new InvalidReturnTypeException(population.getName().getDisplay());
              }
            }
          });
    }

    List<Stratification> stratifications = group.getStratifications();
    if (stratifications != null) {
      stratifications.forEach(
          stratification -> {
            if (StringUtils.isNotBlank(stratification.getCqlDefinition())) {
              String returnType = cqlDefinitionReturnTypes.get(stratification.getCqlDefinition());
              if (!StringUtils.equalsIgnoreCase(returnType, populationBasis)) {
                throw new InvalidReturnTypeException("Stratification(s)");
              }
            }
          });
    }
  }

  /**
   * This method can handle any type that inherits from DefDescPair and is intended to compare the
   * pair with the CQL Definitions provided in the elmJson.
   *
   * @param sde
   * @param elmJson
   * @return true if the def is contained in the elmJson, false otherwise
   */
  public boolean validateDefDescription(DefDescPair sde, String elmJson) {
    boolean result = false;
    try {
      Map<String, String> cqlDefinitionReturnTypes = getCqlDefinitionReturnTypes(elmJson);
      result = cqlDefinitionReturnTypes.containsKey(sde.getDefinition());
    } catch (JsonProcessingException e) {
      log.error("Error reading elmJson", e);
      result = false;
    }

    return result;
  }

  /**
   * This method generates the map of cql definitions & their return types.
   *
   * @param elmJson
   * @return
   * @throws JsonProcessingException
   */
  private Map<String, String> getCqlDefinitionReturnTypes(String elmJson)
      throws JsonProcessingException {
    Map<String, String> returnTypes = new HashMap<>();
    if (StringUtils.isEmpty(elmJson)) {
      return returnTypes;
    }

    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode rootNode = objectMapper.readTree(elmJson);
    ArrayNode allDefinitions = (ArrayNode) rootNode.get("library").get("statements").get("def");
    for (JsonNode node : allDefinitions) {
      if (node.get("resultTypeName") != null) {
        String dataType = node.get("resultTypeName").asText();
        returnTypes.put(node.get("name").asText(), dataType.split("}")[1]);
      } else if (node.get("resultTypeSpecifier") != null) {
        Iterator<JsonNode> typeSpecifierIterator = node.get("resultTypeSpecifier").elements();
        while (typeSpecifierIterator.hasNext()) {
          JsonNode current = typeSpecifierIterator.next();
          if (current.has("type") && current.get("type").asText().equals("NamedTypeSpecifier")) {
            returnTypes.put(node.get("name").asText(), current.get("name").asText().split("}")[1]);
          }
        }
      }
      returnTypes.putIfAbsent(node.get("name").asText(), "NA");
    }
    return returnTypes;
  }
}
