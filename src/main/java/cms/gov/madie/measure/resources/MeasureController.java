package cms.gov.madie.measure.resources;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cms.gov.madie.measure.models.Measure;
import cms.gov.madie.measure.repositories.MeasureRepository;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.RequiredArgsConstructor;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MeasureController {

  @Autowired private final MeasureRepository repository;

  @GetMapping("/measures")
  public ResponseEntity<List<Measure>> getMeasures(
          Principal principal,
          @RequestParam(required = false, defaultValue = "false", name = "currentUser") boolean filterByCurrentUser) {
    final String username = principal.getName();
    List<Measure> measures = filterByCurrentUser ?
            repository.findAllByCreatedBy(username) : repository.findAll();
    return ResponseEntity.ok(measures);
  }

  @GetMapping("/measures/{id}")
  public ResponseEntity<Measure> getMeasure(@PathVariable("id") String id) {
    Optional<Measure> measure = repository.findById(id);
    return measure
        .map(ResponseEntity::ok)
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @PostMapping("/measure")
  public ResponseEntity<Measure> addMeasure(
          @RequestBody @Validated(Measure.ValidationSequence.class) Measure measure, Principal principal) {
    final String username = principal.getName();
    log.info("User [{}] is attempting to create a new measure", username);
    checkDuplicateCqlLibraryName(measure.getCqlLibraryName());

    // Clear ID so that the unique GUID from MongoDB will be applied
    Instant now = Instant.now();
    measure.setId(null);
    measure.setCreatedBy(username);
    measure.setCreatedAt(now);
    measure.setLastModifiedBy(username);
    measure.setLastModifiedAt(now);
    Measure savedMeasure = repository.save(measure);
    log.info("User [{}] successfully created new measure with ID [{}]", username, measure.getId());
    return ResponseEntity.status(HttpStatus.CREATED).body(savedMeasure);
  }

  @PutMapping("/measure")
  public ResponseEntity<String> updateMeasure(
      @RequestBody @Validated(Measure.ValidationSequence.class) Measure measure, Principal principal) {
    ResponseEntity<String> response = ResponseEntity.badRequest().body("Measure does not exist.");
    final String username = principal.getName();

    if (measure.getId() != null) {
      Optional<Measure> persistedMeasure = repository.findById(measure.getId());
      if (persistedMeasure.isPresent()) {
        if (isCqlLibraryNameChanged(measure, persistedMeasure)) {
          checkDuplicateCqlLibraryName(measure.getCqlLibraryName());
        }
        measure.setLastModifiedBy(username);
        measure.setLastModifiedAt(Instant.now());
        // prevent users from overwriting the createdAt/By
        measure.setCreatedAt(persistedMeasure.get().getCreatedAt());
        measure.setCreatedBy(persistedMeasure.get().getCreatedBy());
        repository.save(measure);
        response = ResponseEntity.ok().body("Measure updated successfully.");
      }
    }
    return response;
  }

  private boolean isCqlLibraryNameChanged(Measure measure, Optional<Measure> persistedMeasure) {
    return !Objects.equals(persistedMeasure.get().getCqlLibraryName(), measure.getCqlLibraryName());
  }

  private void checkDuplicateCqlLibraryName(String cqlLibraryName) {
    if (StringUtils.isNotEmpty(cqlLibraryName)
        && repository.findByCqlLibraryName(cqlLibraryName).isPresent()) {
      throw new DuplicateKeyException(
          "cqlLibraryName", "CQL library with given name already exists.");
    }
  }
}
