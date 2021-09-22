package com.semanticbits.measureservice.resources;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.semanticbits.measureservice.models.Measure;
import com.semanticbits.measureservice.repositories.MeasureRepository;

@RestController
public class MeasureController {

  @Autowired private MeasureRepository repository;

  @GetMapping
  public List<Measure> getMeasures() {
    List<Measure> measures = repository.findAll();
    return measures;
  }
}
