package cms.gov.madie.measure.poc.dto;

import cms.gov.madie.measure.poc.dto.model.FhirMeasure;
import cms.gov.madie.measure.poc.dto.model.Group;
import cms.gov.madie.measure.poc.dto.model.Measure;
import cms.gov.madie.measure.poc.dto.model.QDMMeasure;
import gov.cms.madie.models.common.ModelType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class Translator {

  public Measure translate(MeasureDTO measureDTO) {
    if (measureDTO.getModelType().equals(ModelType.QDM_5_6.toString())) {
      return translateQdm(measureDTO);
    } else if (measureDTO.getModelType().equals(ModelType.QI_CORE.toString())) {
      return translateFhir(measureDTO);
    }

    throw new UnsupportedOperationException("Wrong Model Type used");
  }

  private Measure translateFhir(MeasureDTO measureDTO) {
    return FhirMeasure.builder()
        .name(measureDTO.getName())
        .groups(translateGroup(measureDTO.getGroups()))
        .build();
  }

  private List<Group> translateGroup(List<GroupDTO> groups) {

    return groups.stream()
        .map(dto -> Group.builder().scoring(dto.getScoring()).build())
        .collect(Collectors.toList());
  }

  private Measure translateQdm(MeasureDTO measureDTO) {
    return QDMMeasure.builder().name(measureDTO.getName()).scoring(measureDTO.getScoring()).build();
  }
}
