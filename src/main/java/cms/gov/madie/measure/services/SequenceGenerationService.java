package cms.gov.madie.measure.services;

import cms.gov.madie.measure.repositories.GeneratorRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class SequenceGenerationService {
  private final GeneratorRepository generatorRepository;

  public int generateSequenceNumber(String sequenceName) {
    return generatorRepository.findAndModify(sequenceName);
  }
}
