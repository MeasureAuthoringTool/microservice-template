package cms.gov.madie.measure.resources;

import java.security.Principal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import cms.gov.madie.measure.exceptions.InvalidIdException;
import cms.gov.madie.measure.models.Group;
import cms.gov.madie.measure.services.MeasureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
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

import javax.validation.Valid;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MeasureController {

  @Autowired private final MeasureRepository repository;
  @Autowired private final MeasureService measureService;

  @GetMapping("/measures")
  public ResponseEntity<Page<Measure>> getMeasures(
      Principal principal,
      @RequestParam(required = false, defaultValue = "false", name = "currentUser")
          boolean filterByCurrentUser,
      @RequestParam(required = false, defaultValue = "10", name = "limit") int limit,
      @RequestParam(required = false, defaultValue = "0", name = "page") int page) {
    final String username = principal.getName();
    final Pageable pageReq = PageRequest.of(page, limit, Sort.by("lastModifiedAt").descending());
    Page<Measure> measures =
        filterByCurrentUser
            ? repository.findAllByCreatedBy(username, pageReq)
            : repository.findAll(pageReq);
    // : repository.findAllByIdNotNull(page);
    // return ResponseEntity;
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
      @RequestBody @Validated(Measure.ValidationSequence.class) Measure measure,
      Principal principal) {
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

  @PutMapping("/measures/{id}")
  public ResponseEntity<String> updateMeasure(
      @PathVariable("id") String id,
      @RequestBody @Validated(Measure.ValidationSequence.class) Measure measure,
      Principal principal) {
    ResponseEntity<String> response = ResponseEntity.badRequest().body("Measure does not exist.");
    final String username = principal.getName();
    if (id == null || id.isEmpty() || !id.equals(measure.getId())) {
      log.info("got invalid id [{}] vs measureId: [{}]", id, measure.getId());
      throw new InvalidIdException("Measure", "Update (PUT)", "(PUT [base]/[resource]/[id])");
    }

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

  @PostMapping("/measures/{measureId}/groups")
  public ResponseEntity<Group> createGroup(
      @RequestBody @Valid Group group, @PathVariable String measureId, Principal principal) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(measureService.createOrUpdateGroup(group, measureId, principal.getName()));
  }

  @PutMapping("/measures/{measureId}/groups")
  public ResponseEntity<Group> updateGroup(
      @RequestBody @Valid Group group, @PathVariable String measureId, Principal principal) {
    return ResponseEntity.ok(
        measureService.createOrUpdateGroup(group, measureId, principal.getName()));
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
