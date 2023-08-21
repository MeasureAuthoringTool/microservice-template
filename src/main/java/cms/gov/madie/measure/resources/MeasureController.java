package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.exceptions.InvalidDraftStatusException;
import cms.gov.madie.measure.exceptions.InvalidIdException;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.exceptions.UnauthorizedException;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.repositories.MeasureSetRepository;
import cms.gov.madie.measure.services.ActionLogService;
import cms.gov.madie.measure.services.GroupService;
import cms.gov.madie.measure.services.MeasureService;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MeasureController {

  private final MeasureRepository repository;
  private final MeasureService measureService;
  private final GroupService groupService;
  private final ActionLogService actionLogService;
  private final MeasureSetRepository measureSetRepository;

  @GetMapping("/measures/draftstatus")
  public ResponseEntity<Map<String, Boolean>> getDraftStatuses(
      @RequestParam(required = true, name = "measureSetIds") List<String> measureSetIds) {
    Map<String, Boolean> results = new HashMap<>();
    results = measureService.getMeasureDrafts(measureSetIds);
    return ResponseEntity.status(HttpStatus.CREATED).body(results);
  }

  @GetMapping("/measures")
  public ResponseEntity<Page<Measure>> getMeasures(
      Principal principal,
      @RequestParam(required = false, defaultValue = "false", name = "currentUser")
          boolean filterByCurrentUser,
      @RequestParam(required = false, defaultValue = "10", name = "limit") int limit,
      @RequestParam(required = false, defaultValue = "0", name = "page") int page) {
    final String username = principal.getName();
    Page<Measure> measures;
    final Pageable pageReq = PageRequest.of(page, limit, Sort.by("lastModifiedAt").descending());
    measures = measureService.getMeasures(filterByCurrentUser, pageReq, username);
    measures.map(
        measure -> {
          MeasureSet measureSet =
              measureSetRepository.findByMeasureSetId(measure.getMeasureSetId()).orElse(null);
          measure.setMeasureSet(measureSet);
          return measure;
        });
    return ResponseEntity.ok(measures);
  }

  @GetMapping("/measures/{id}")
  public ResponseEntity<Measure> getMeasure(@PathVariable("id") String id) {
    Optional<Measure> measureOptional = repository.findByIdAndActive(id, true);
    if (measureOptional.isPresent()) {
      Measure measure = measureOptional.get();
      MeasureSet measureSet =
          measureSetRepository.findByMeasureSetId(measure.getMeasureSetId()).orElse(null);
      measure.setMeasureSet(measureSet);
      return ResponseEntity.status(HttpStatus.OK).body(measure);
    }
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
  }

  @PostMapping("/measure")
  public ResponseEntity<Measure> addMeasure(
      @RequestBody @Validated(Measure.ValidationSequence.class) Measure measure,
      Principal principal,
      @RequestHeader("Authorization") String accessToken) {
    final String username = principal.getName();
    Measure savedMeasure = measureService.createMeasure(measure, username, accessToken);
    return ResponseEntity.status(HttpStatus.CREATED).body(savedMeasure);
  }

  @PutMapping("/measures/{id}")
  public ResponseEntity<Measure> updateMeasure(
      @PathVariable("id") String id,
      @RequestBody @Validated(Measure.ValidationSequence.class) Measure measure,
      Principal principal,
      @RequestHeader("Authorization") String accessToken) {
    ResponseEntity<Measure> response;
    final String username = principal.getName();
    if (id == null || id.isEmpty() || !id.equals(measure.getId())) {
      log.info("got invalid id [{}] vs measureId: [{}]", id, measure.getId());
      throw new InvalidIdException("Measure", "Update (PUT)", "(PUT [base]/[resource]/[id])");
    }

    log.info("getMeasureId [{}]", id);

    final Measure existingMeasure = measureService.findMeasureById(id);

    if (existingMeasure != null) {
      if (username != null && existingMeasure.getCreatedBy() != null) {
        log.info("got username [{}] vs createdBy: [{}]", username, existingMeasure.getCreatedBy());
        // either owner or shared-with role
        measureService.verifyAuthorization(username, existingMeasure);

        if (!existingMeasure.getMeasureMetaData().isDraft()) {
          throw new InvalidDraftStatusException(measure.getId());
        }

        // no user can update a soft-deleted measure
        if (!existingMeasure.isActive()) {
          throw new UnauthorizedException("Measure", existingMeasure.getId(), username);
        }
        // shared user should be able to edit Measure but won’t have delete access, only owner can
        // delete
        if (!measure.isActive()) {
          measureService.verifyAuthorization(username, measure, null);
        }
      }

      response =
          ResponseEntity.ok()
              .body(measureService.updateMeasure(existingMeasure, username, measure, accessToken));
      if (!measure.isActive()) {
        actionLogService.logAction(id, Measure.class, ActionType.DELETED, username);
      } else {
        actionLogService.logAction(id, Measure.class, ActionType.UPDATED, username);
      }
    } else {
      throw new ResourceNotFoundException("Measure", id);
    }

    return response;
  }

  @PutMapping("/measures/{id}/grant")
  @PreAuthorize("#request.getHeader('api-key') == #apiKey")
  public ResponseEntity<String> grantAccess(
      HttpServletRequest request,
      @PathVariable("id") String id,
      @RequestParam(required = true, name = "userid") String userid,
      @Value("${lambda-api-key}") String apiKey) {
    ResponseEntity<String> response = ResponseEntity.badRequest().body("Measure does not exist.");

    log.info("getMeasureId [{}] using apiKey ", id, "apikey");

    if (measureService.grantAccess(id, userid)) {
      response =
          ResponseEntity.ok()
              .body(String.format("%s granted access to Measure successfully.", userid));
      actionLogService.logAction(id, Measure.class, ActionType.UPDATED, "apiKey");
    }

    return response;
  }

  @PutMapping("/measures/{id}/ownership")
  @PreAuthorize("#request.getHeader('api-key') == #apiKey")
  public ResponseEntity<String> changeOwnership(
      HttpServletRequest request,
      @PathVariable("id") String id,
      @RequestParam(required = true, name = "userid") String userid,
      @Value("${lambda-api-key}") String apiKey) {
    ResponseEntity<String> response = ResponseEntity.badRequest().body("Measure does not exist.");

    log.info("getMeasureId [{}] using apiKey ", id, "apikey");

    if (measureService.changeOwnership(id, userid)) {
      response =
          ResponseEntity.ok()
              .body(String.format("%s granted ownership to Measure successfully.", userid));
      actionLogService.logAction(id, Measure.class, ActionType.UPDATED, "apiKey");
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
      @RequestBody @Validated(Measure.ValidationSequence.class) Group group,
      @PathVariable String measureId,
      Principal principal) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(groupService.createOrUpdateGroup(group, measureId, principal.getName()));
  }

  @PutMapping("/measures/{measureId}/groups")
  public ResponseEntity<Group> updateGroup(
      @RequestBody @Validated(Measure.ValidationSequence.class) Group group,
      @PathVariable String measureId,
      Principal principal) {
    return ResponseEntity.ok(
        groupService.createOrUpdateGroup(group, measureId, principal.getName()));
  }

  @DeleteMapping("/measures/{measureId}/groups/{groupId}")
  public ResponseEntity<Measure> deleteMeasureGroup(
      @RequestBody @PathVariable String measureId,
      @PathVariable String groupId,
      Principal principal) {

    log.info(
        "User [{}] is attempting to delete a group with Id [{}] from measure [{}]",
        principal.getName(),
        groupId,
        measureId);
    return ResponseEntity.ok(
        groupService.deleteMeasureGroup(measureId, groupId, principal.getName()));
  }

  @GetMapping("/measures/search/{criteria}")
  public ResponseEntity<Page<Measure>> findAllByMeasureNameOrEcqmTitle(
      Principal principal,
      @RequestParam(required = false, defaultValue = "false", name = "currentUser")
          boolean filterByCurrentUser,
      @PathVariable("criteria") String criteria,
      @RequestParam(required = false, defaultValue = "10", name = "limit") int limit,
      @RequestParam(required = false, defaultValue = "0", name = "page") int page) {

    final String username = principal.getName();
    final Pageable pageReq = PageRequest.of(page, limit, Sort.by("lastModifiedAt").descending());

    Page<Measure> measures =
        measureService.getMeasuresByCriteria(filterByCurrentUser, pageReq, username, criteria);
    measures.map(
        measure -> {
          MeasureSet measureSet =
              measureSetRepository.findByMeasureSetId(measure.getMeasureSetId()).orElse(null);
          measure.setMeasureSet(measureSet);
          return measure;
        });

    return ResponseEntity.ok(measures);
  }
}
