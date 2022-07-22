package cms.gov.madie.measure.services;

import java.time.Instant;

import org.springframework.stereotype.Service;

import cms.gov.madie.measure.repositories.ActionLogRepository;
import gov.cms.madie.models.common.Action;
import gov.cms.madie.models.common.ActionType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ActionLogService {
  private final ActionLogRepository actionLogRepository;

  public boolean logAction(
      final String targetId, final ActionType actionType, final String userId) {
    return actionLogRepository.pushEvent(
        targetId,
        Action.builder()
            .actionType(actionType)
            .performedBy(userId)
            .performedAt(Instant.now())
            .build());
  }
}
