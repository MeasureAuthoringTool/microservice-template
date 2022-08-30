package cms.gov.madie.measure.validations;

import cms.gov.madie.measure.exceptions.InvalidIdException;
import cms.gov.madie.measure.exceptions.InvalidReturnTypeException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.Stratification;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CqlDefinitionReturnTypeValidator {

  /**
   * Validate cql definition return types of a group against population basis. Group can have cql
   * definitions for populations, stratifications and observations
   */
  public void validateCqlDefinitionReturnTypes(Group group, String elmJson)
      throws JsonProcessingException {
    Map<String, String> cqlDefinitionReturnTypes = getCqlDefinitionReturnTypes(elmJson);
    if (cqlDefinitionReturnTypes.isEmpty()) {
      throw new InvalidIdException("No elm json available");
    }

    List<Population> populations = group.getPopulations();
    String populationBasis = group.getPopulationBasis().replaceAll("\\s", "");
    if (populations != null) {
      populations.forEach(
          population -> {
            if (StringUtils.isNotBlank(population.getDefinition())) {
              String returnType = cqlDefinitionReturnTypes.get(population.getDefinition());
              if (!StringUtils.equals(returnType, populationBasis)) {
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
              if (!StringUtils.equals(returnType, populationBasis)) {
                throw new InvalidReturnTypeException("Stratification(s)");
              }
            }
          });
    }
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
    Iterator<JsonNode> nodeIterator = allDefinitions.iterator();
    while (nodeIterator.hasNext()) {
      JsonNode node = nodeIterator.next();
      if (node.get("resultTypeName") != null) {
        String dataType = node.get("resultTypeName").asText();
        returnTypes.put(node.get("name").asText(), dataType.split("}")[1]);
      } else if (node.get("resultTypeSpecifier") != null) {
        String type = node.get("resultTypeSpecifier").get("elementType").get("type").asText();
        if ("NamedTypeSpecifier".equals(type)) {
          String dataType = node.get("resultTypeSpecifier").get("elementType").get("name").asText();
          returnTypes.put(node.get("name").asText(), dataType.split("}")[1]);
        } else {
          returnTypes.put(node.get("name").asText(), "NA");
        }
      } else {
        returnTypes.put(node.get("name").asText(), "NA");
      }
    }
    return returnTypes;
  }
}
