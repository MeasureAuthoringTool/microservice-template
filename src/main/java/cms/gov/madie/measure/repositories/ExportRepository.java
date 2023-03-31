package cms.gov.madie.measure.repositories;


import gov.cms.madie.models.measure.Export;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExportRepository extends MongoRepository<Export, String> {}
