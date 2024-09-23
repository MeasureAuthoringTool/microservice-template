package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.dto.MeasureListDTO;
import cms.gov.madie.measure.exceptions.*;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.repositories.MeasureSetRepository;
import cms.gov.madie.measure.services.ActionLogService;
import cms.gov.madie.measure.services.GroupService;
import cms.gov.madie.measure.services.MeasureService;
import cms.gov.madie.measure.services.MeasureSetService;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.dto.LibraryUsage;
import gov.cms.madie.models.measure.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
  private final MeasureSetService measureSetService;

  @GetMapping("/measures/draftstatus")
  public ResponseEntity<Map<String, Boolean>> getDraftStatuses(
      @RequestParam(required = true, name = "measureSetIds") List<String> measureSetIds) {
    Map<String, Boolean> results = new HashMap<>();
    results = measureService.getMeasureDrafts(measureSetIds);
    return ResponseEntity.status(HttpStatus.CREATED).body(results);
  }

  @GetMapping("/measures")
  public ResponseEntity<Page<MeasureListDTO>> getMeasures(
      Principal principal,
      @RequestParam(required = false, defaultValue = "false", name = "currentUser")
          boolean filterByCurrentUser,
      @RequestParam(required = false, defaultValue = "10", name = "limit") int limit,
      @RequestParam(required = false, defaultValue = "0", name = "page") int page) {
    final String username = principal.getName();
    Page<MeasureListDTO> measures;
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
      @RequestParam(required = false, defaultValue = "true", name = "addDefaultCQL")
          boolean addDefaultCQL,
      Principal principal,
      @RequestHeader("Authorization") String accessToken) {
    final String username = principal.getName();
    Measure savedMeasure =
        measureService.createMeasure(measure, username, accessToken, addDefaultCQL);
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

  @DeleteMapping("/measures/{id}/delete")
  public ResponseEntity<Measure> deactivateMeasure(
      @PathVariable("id") String id, Principal principal) {
    if (StringUtils.isBlank(id)) {
      log.info("Invalid measure id: " + id);
      throw new InvalidIdException("Measure", "Delete (DELETE)", "(DELETE [base]/[resource]/[id])");
    }
    return ResponseEntity.ok()
        .body(
            measureService.deactivateMeasure(
                measureService.findMeasureById(id), principal.getName()));
  }

  @PutMapping("/measures/{id}/grant")
  @PreAuthorize("#request.getHeader('api-key') == #apiKey")
  public ResponseEntity<String> grantAccess(
      HttpServletRequest request,
      @PathVariable("id") String id,
      @RequestParam(required = true, name = "userid") String userid,
      @Value("${admin-api-key}") String apiKey) {
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
      @Value("${admin-api-key}") String apiKey) {
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

  @PostMapping("/measures/{measureId}/groups/{groupId}/stratification")
  public ResponseEntity<Stratification> createStratification(
      @RequestBody Stratification stratification,
      @PathVariable String measureId,
      @PathVariable String groupId,
      Principal principal) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            groupService.createOrUpdateStratification(
                groupId, measureId, stratification, principal.getName()));
  }

  @PutMapping("/measures/{measureId}/groups/{groupId}/stratification")
  public ResponseEntity<Stratification> updateStratification(
      @RequestBody Stratification stratification,
      @PathVariable String measureId,
      @PathVariable String groupId,
      Principal principal) {
    return ResponseEntity.ok(
        groupService.createOrUpdateStratification(
            groupId, measureId, stratification, principal.getName()));
  }

  @DeleteMapping("/measures/{measureId}/groups/{groupId}/stratification/{stratificationId}")
  public ResponseEntity<Measure> deleteStratification(
      @RequestBody @PathVariable String measureId,
      @PathVariable String groupId,
      @PathVariable String stratificationId,
      Principal principal) {

    log.info(
        "User [{}] is attempting to delete a group with Id [{}] from measure [{}]",
        principal.getName(),
        groupId,
        measureId);
    return ResponseEntity.ok(
        groupService.deleteStratification(
            measureId, groupId, stratificationId, principal.getName()));
  }

  @GetMapping("/measures/search")
  public ResponseEntity<Page<MeasureListDTO>> findAllByMeasureNameOrEcqmTitle(
      Principal principal,
      @RequestParam(required = false, defaultValue = "false", name = "currentUser")
          boolean filterByCurrentUser,
      @RequestParam(required = false, name = "query") String query,
      @RequestParam(required = false, defaultValue = "10", name = "limit") int limit,
      @RequestParam(required = false, defaultValue = "0", name = "page") int page) {

    final String username = principal.getName();
    final Pageable pageReq = PageRequest.of(page, limit, Sort.by("lastModifiedAt").descending());

    // We need to decode the encoded strings we send over or we can't find stuff
    String decodedQuery = URLDecoder.decode(query, StandardCharsets.UTF_8);
    Page<MeasureListDTO> measures =
        measureService.getMeasuresByCriteria(filterByCurrentUser, pageReq, username, decodedQuery);
    measures.map(
        measure -> {
          MeasureSet measureSet =
              measureSetRepository.findByMeasureSetId(measure.getMeasureSetId()).orElse(null);
          measure.setMeasureSet(measureSet);
          return measure;
        });

    return ResponseEntity.ok(measures);
  }

  @PutMapping("/measures/{measureSetId}/create-cms-id")
  public ResponseEntity<MeasureSet> createCmsId(
      @PathVariable String measureSetId, Principal principal) {
    measureService.verifyAuthorizationByMeasureSetId(principal.getName(), measureSetId, true);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(measureSetService.createAndUpdateCmsId(measureSetId, principal.getName()));
  }

  @PutMapping("/measures/cms-id-association")
  public ResponseEntity<MeasureSet> associateCmsId(
      Principal principal,
      @RequestParam String qiCoreMeasureId,
      @RequestParam String qdmMeasureId,
      @RequestParam(defaultValue = "false") boolean copyMetaData) {
    return ResponseEntity.ok(
        measureService.associateCmsId(
            principal.getName(), qiCoreMeasureId, qdmMeasureId, copyMetaData));
  }

  @GetMapping(
      value = "/measures/library/usage",
      produces = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<List<LibraryUsage>> getLibraryUsage(@RequestParam String libraryName) {
    return ResponseEntity.ok().body(measureService.findLibraryUsage(libraryName));
  }
}
