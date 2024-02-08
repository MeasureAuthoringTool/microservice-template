package cms.gov.madie.measure.repositories;

import gov.cms.madie.models.measure.Generator;

import java.util.Optional;

public interface GeneratorRepository {
  Optional<Generator> findAndModify(String sequenceName);

  boolean existsBySequenceName(String sequenceName);
}
