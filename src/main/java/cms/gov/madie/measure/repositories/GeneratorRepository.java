package cms.gov.madie.measure.repositories;

public interface GeneratorRepository {
  int findAndModify(String sequenceName);
}
