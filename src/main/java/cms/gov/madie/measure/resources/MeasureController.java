package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.utils.ControllerUtil;
import gov.cms.madie.models.access.RoleEnum;

import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
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
import cms.gov.madie.measure.exceptions.InvalidIdException;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.exceptions.UnauthorizedException;
import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.services.ActionLogService;
import cms.gov.madie.measure.services.GroupService;
import cms.gov.madie.measure.services.MeasureService;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureMetaData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MeasureController {

  private final MeasureRepository repository;
  private final MeasureService measureService;
  private final GroupService groupService;
  private final ActionLogService actionLogService;

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
            ? repository.findAllByCreatedByAndActiveOrShared(
                username, true, RoleEnum.SHARED_WITH.toString(), pageReq)
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
    measureService.validateMeasurementPeriod(
        measure.getMeasurementPeriodStart(), measure.getMeasurementPeriodEnd());

    // Clear ID so that the unique GUID from MongoDB will be applied
    Instant now = Instant.now();
    measure.setId(null);
    measure.setCreatedBy(username);
    measure.setCreatedAt(now);
    measure.setLastModifiedBy(username);
    measure.setLastModifiedAt(now);
    measure.setVersion(new Version(0, 0, 0));

    if (measure.getMeasureMetaData() != null) {
      measure.getMeasureMetaData().setDraft(true);
    } else {
      MeasureMetaData metaData = new MeasureMetaData();
      metaData.setDraft(true);
      measure.setMeasureMetaData(metaData);
    }

    Measure savedMeasure = repository.save(measure);
    log.info("User [{}] successfully created new measure with ID [{}]", username, measure.getId());

    actionLogService.logAction(savedMeasure.getId(), Measure.class, ActionType.CREATED, username);

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

    Optional<Measure> persistedMeasureOpt = repository.findById(id);

    if (persistedMeasureOpt.isPresent()) {
      final Measure existingMeasure = persistedMeasureOpt.get();
      if (username != null && existingMeasure.getCreatedBy() != null) {
        log.info("got username [{}] vs createdBy: [{}]", username, existingMeasure.getCreatedBy());
        // either owner or shared-with role
        ControllerUtil.verifyAuthorization(username, existingMeasure);

        // no user can update a soft-deleted measure
        if (!existingMeasure.isActive()) {
          throw new UnauthorizedException("Measure", existingMeasure.getId(), username);
        }
        // shared user should be able to edit Measure but won’t have delete access
        if (!measure.isActive()) {
          measureService.checkDeletionCredentials(username, existingMeasure.getCreatedBy());
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
      @Value("${lambda-api-key}") String apiKey,
      Principal principal) {
    ResponseEntity<String> response = ResponseEntity.badRequest().body("Measure does not exist.");

    log.info("getMeasureId [{}] using apiKey ", id, "apikey");

    if (measureService.grantAccess(id, userid, apiKey)) {
      response =
          ResponseEntity.ok()
              .body(String.format("%s granted access to Measure successfully.", userid));
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
      @RequestParam(required = false, defaultValue = "0", name = "page") int page)
      throws UnsupportedEncodingException {

    final String username = principal.getName();
    final Pageable pageReq = PageRequest.of(page, limit, Sort.by("lastModifiedAt").descending());
    Page<Measure> measures =
        filterByCurrentUser
            ? repository.findAllByMeasureNameOrEcqmTitleForCurrentUser(criteria, pageReq, username)
            : repository.findAllByMeasureNameOrEcqmTitle(criteria, pageReq);
    return ResponseEntity.ok(measures);
  }
}
