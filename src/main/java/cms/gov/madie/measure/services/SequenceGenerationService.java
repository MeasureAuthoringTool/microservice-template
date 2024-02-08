package cms.gov.madie.measure.services;

import cms.gov.madie.measure.repositories.GeneratorRepository;
import gov.cms.madie.models.measure.Generator;
import gov.cms.madie.models.measure.MeasureSet;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class SequenceGenerationService {
  private final GeneratorRepository generatorRepository;

  public MeasureSet incrementSequence(String sequenceName) {
    //        final Generators generatorValue=new Generators();
    //        generatorValue.setName("Cms ID");
    //        generatorValue.setCurVal(1);
    boolean ifPresent = generatorRepository.existsBySequenceName(sequenceName);
    // if it doesn't exist do we need to create new record
    // when are we saving this incremented value to the measureSet
    if (ifPresent) {
      Optional<Generator> updatedSequenceNumber = generatorRepository.findAndModify(sequenceName);
      System.out.println(updatedSequenceNumber);
    }
    return new MeasureSet();
  }
}
