package cms.gov.madie.measure.services;

import cms.gov.madie.measure.repositories.MeasureActionLogRepository;
import cms.gov.madie.measure.utils.ActionLogCollectionType;
import gov.cms.madie.models.common.Action;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.common.AccessControlAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionLogService {

  private final MeasureActionLogRepository actionLogRepository;

  public boolean logAction(
      final String targetId,
      Class targetClass,
      final ActionType actionType,
      final String userId,
      final String... additionalActionMessage) {
    final String collection = ActionLogCollectionType.getCollectionNameForClazz(targetClass);

    return actionLogRepository.pushEvent(
        targetId,
        Action.builder()
            .actionType(actionType)
            .performedBy(userId)
            .performedAt(Instant.now())
            .additionalActionMessage(Arrays.toString(additionalActionMessage))
            .build(),
        collection);
  }

  public boolean logAccessControlAction(
      final String targetId,
      Class targetClass,
      final ActionType actionType,
      final String userId,
      final String sharedWith,
      final String... additionalActionMessage) {
    final String collection = ActionLogCollectionType.getCollectionNameForClazz(targetClass);

    return actionLogRepository.pushEvent(
        targetId,
        AccessControlAction.builder()
            .actionType(actionType)
            .performedBy(userId)
            .performedAt(Instant.now())
            .sharedWith(sharedWith)
            .additionalActionMessage(Arrays.toString(additionalActionMessage))
            .build(),
        collection);
  }
}
