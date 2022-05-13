package cms.gov.madie.measure.resources;

import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import cms.gov.madie.measure.exceptions.InvalidIdException;
import cms.gov.madie.measure.exceptions.InvalidResourceBundleStateException;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.exceptions.UnauthorizedException;
import cms.gov.madie.measure.models.Group;
import cms.gov.madie.measure.services.MeasureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cms.gov.madie.measure.models.Measure;
import cms.gov.madie.measure.repositories.MeasureRepository;
import lombok.RequiredArgsConstructor;

import javax.validation.Valid;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MeasureController {

  private final MeasureRepository repository;
  private final MeasureService measureService;

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
            ? repository.findAllByCreatedByAndActive(username, true, pageReq)
            : repository.findAllByActive(true, pageReq);
    return ResponseEntity.ok(measures);
  }

  @GetMapping("/measures/{id}")
  public ResponseEntity<Measure> getMeasure(@PathVariable("id") String id) {
    Optional<Measure> measure = repository.findByIdAndActive(id, true);
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
    measureService.checkDuplicateCqlLibraryName(measure.getCqlLibraryName());

    // Clear ID so that the unique GUID from MongoDB will be applied
    Instant now = Instant.now();
    measure.setId(null);
    measure.setCreatedBy(username);
    measure.setCreatedAt(now);
    measure.setLastModifiedBy(username);
    measure.setLastModifiedAt(now);

    //    int nextCalendarYear = LocalDate.now().plusYears(1).getYear();
    //    measure.setMeasurementPeriodStart(LocalDate.of(nextCalendarYear, Month.JANUARY, 1));
    //    measure.setMeasurementPeriodEnd(LocalDate.of(nextCalendarYear, Month.DECEMBER, 31));
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
    if (username != null && measure.getCreatedBy() != null && !measure.isActive()) {
      log.info("got username [{}] vs createdBy: [{}]", username, measure.getCreatedBy());
      measureService.checkDeletionCredentials(username, measure.getCreatedBy());
    }

    if (measure.getId() != null) {
      log.info("getMeasureId [{}]", measure.getId());
      Optional<Measure> persistedMeasure = repository.findById(measure.getId());
      if (persistedMeasure.isPresent()) {
        if (isCqlLibraryNameChanged(measure, persistedMeasure)) {
          measureService.checkDuplicateCqlLibraryName(measure.getCqlLibraryName());
        }
        if (isCqlLibraryMeasurementPeriodChanged(measure, persistedMeasure)) {
          measureService.checkMeasurementPeriodValidity(
              measure.getMeasurementPeriodStart(), measure.getMeasurementPeriodEnd());
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

  @GetMapping("/measures/{measureId}/groups")
  public ResponseEntity<List<Group>> getGroups(@PathVariable String measureId) {
    return repository
        .findById(measureId)
        .map(
            measure -> {
              List<Group> groups = measure.getGroups() == null ? List.of() : measure.getGroups();
              return ResponseEntity.ok(groups);
            })
        .orElseThrow(() -> new ResourceNotFoundException("Measure", measureId));
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

  @GetMapping(path = "/measures/{measureId}/bundles", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> getMeasureBundle(
      @PathVariable String measureId,
      Principal principal,
      @RequestHeader("Authorization") String accessToken) {
    Optional<Measure> measureOptional = repository.findById(measureId);
    if (measureOptional.isEmpty()) {
      throw new ResourceNotFoundException("Measure", measureId);
    }
    Measure measure = measureOptional.get();
    if (!principal.getName().equals(measure.getCreatedBy())) {
      throw new UnauthorizedException("Measure", measureId, principal.getName());
    }
    if (measure.isCqlErrors()) {
      throw new InvalidResourceBundleStateException("Measure", measureId);
    }
    return ResponseEntity.ok(measureService.bundleMeasure(measure, accessToken));
  }

  private boolean isCqlLibraryNameChanged(Measure measure, Optional<Measure> persistedMeasure) {
    return !Objects.equals(persistedMeasure.get().getCqlLibraryName(), measure.getCqlLibraryName());
  }

  private boolean isCqlLibraryMeasurementPeriodChanged(
      Measure measure, Optional<Measure> persistedMeasure) {
    return !Objects.equals(
            persistedMeasure.get().getMeasurementPeriodStart(), measure.getMeasurementPeriodStart())
        || !Objects.equals(
            persistedMeasure.get().getMeasurementPeriodEnd(), measure.getMeasurementPeriodEnd());
  }
}
