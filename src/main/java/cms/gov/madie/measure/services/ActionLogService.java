package cms.gov.madie.measure.services;

import cms.gov.madie.measure.repositories.MeasureActionLogRepository;
import cms.gov.madie.measure.utils.ActionLogCollectionType;
import gov.cms.madie.models.common.Action;
import gov.cms.madie.models.common.ActionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Service
public class ActionLogService {

  private final MeasureActionLogRepository actionLogRepository;

  public boolean logAction(
      final String targetId, Class targetClass, final ActionType actionType, final String userId) {
    final String collection = ActionLogCollectionType.getCollectionNameForClazz(targetClass);

    return actionLogRepository.pushEvent(
        targetId,
        Action.builder()
            .actionType(actionType)
            .performedBy(userId)
            .performedAt(Instant.now())
            .build(),
        collection);
  }
}
