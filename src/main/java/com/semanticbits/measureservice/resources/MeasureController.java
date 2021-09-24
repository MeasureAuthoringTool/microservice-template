package com.semanticbits.measureservice.resources;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.semanticbits.measureservice.models.Measure;
import com.semanticbits.measureservice.repositories.MeasureRepository;

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
