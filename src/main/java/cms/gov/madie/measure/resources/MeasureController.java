package cms.gov.madie.measure.resources;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import cms.gov.madie.measure.models.Measure;
import cms.gov.madie.measure.repositories.MeasureRepository;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MeasureController {

  @Autowired private final MeasureRepository repository;

  @GetMapping("/measures")
  public ResponseEntity<List<Measure>> getMeasures() {
    List<Measure> measures = repository.findAll();
    return ResponseEntity.ok(measures);
  }

  @PostMapping("/measure")
  public ResponseEntity<Measure> addMeasure(@RequestBody Measure measure) {
    // Clear ID so that the unique GUID from MongoDB will be applied
    measure.setId(null);
    Measure savedMeasure = repository.save(measure);

    return ResponseEntity.status(HttpStatus.CREATED).body(savedMeasure);
  }
}
