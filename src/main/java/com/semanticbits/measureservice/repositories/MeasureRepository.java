package com.semanticbits.measureservice.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.semanticbits.measureservice.models.Measure;

public interface MeasureRepository extends MongoRepository<Measure, String> {}
