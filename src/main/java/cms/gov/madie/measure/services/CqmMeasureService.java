package cms.gov.madie.measure.services;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import cms.gov.madie.measure.repositories.CqmMeasureRepository;
import gov.cms.madie.models.cqm.CqmMeasure;
import gov.cms.madie.models.measure.Measure;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class CqmMeasureService {

  private final MeasureService measureService;
  private final QdmPackageService qdmPackageService;
  private final RestTemplate qdmServiceRestTemplate;
  private final CqmMeasureRepository cqmMeasureRepo;

  public CqmMeasure getCqmMeasure(String measureId, String accessToken) {

    // get measure.. determine whether it was versioned
    Measure measure = measureService.findMeasureById(measureId);

    CqmMeasure cqmMeasure;
    if (measure.getMeasureMetaData().isDraft()) {
      // if not versioned, Convert Measure to CqmMeasure with madie-qdm-service
      cqmMeasure = qdmPackageService.convertCqm(measure, accessToken);
    } else {
      // otherwise, get the CqmMeasure from the repo
      cqmMeasure =
          cqmMeasureRepo.findByHqmfSetIdAndHqmfVersionNumber(
              measure.getMeasureSetId(), measure.getVersionId());
    }
    return cqmMeasure;
  }
}
