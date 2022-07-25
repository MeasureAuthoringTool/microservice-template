package cms.gov.madie.measure.service;

import cms.gov.madie.measure.repositories.MeasureActionLogRepository;
import gov.cms.madie.models.common.Action;
import gov.cms.madie.models.common.ActionType;
import gov.cms.madie.models.measure.Measure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cms.gov.madie.measure.repositories.ActionLogRepository;
import cms.gov.madie.measure.services.ActionLogService;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ActionLogServiceTest {
  @Mock MeasureActionLogRepository actionLogRepository;

  @InjectMocks ActionLogService actionLogService;

  @Captor private ArgumentCaptor<Action> actionArgumentCaptor;

  @Captor private ArgumentCaptor<String> targetIdArgumentCaptor;
  @Captor private ArgumentCaptor<String> collectionArgumentCaptor;

  @Test
  void testLogActionReturnsTrue() {
    when(actionLogRepository.pushEvent(anyString(), any(Action.class), anyString()))
        .thenReturn(true);
    boolean output =
        actionLogService.logAction("TARGET_ID", Measure.class, ActionType.CREATED, "testUser");
    assertThat(output, is(true));
    verify(actionLogRepository, times(1))
        .pushEvent(
            targetIdArgumentCaptor.capture(),
            actionArgumentCaptor.capture(),
            collectionArgumentCaptor.capture());
    assertThat(targetIdArgumentCaptor.getValue(), is(equalTo("TARGET_ID")));
    Action value = actionArgumentCaptor.getValue();
    assertThat(value, is(notNullValue()));
    assertThat(value.getActionType(), is(equalTo(ActionType.CREATED)));
    assertThat(value.getPerformedBy(), is(equalTo("testUser")));
  }

  @Test
  void testLogActionReturnsFalse() {
    when(actionLogRepository.pushEvent(anyString(), any(Action.class), anyString()))
        .thenReturn(false);
    boolean output =
        actionLogService.logAction("TARGET_ID", Measure.class, ActionType.DELETED, "testUser");
    assertThat(output, is(false));
    verify(actionLogRepository, times(1))
        .pushEvent(
            targetIdArgumentCaptor.capture(),
            actionArgumentCaptor.capture(),
            collectionArgumentCaptor.capture());
    assertThat(targetIdArgumentCaptor.getValue(), is(equalTo("TARGET_ID")));
    Action value = actionArgumentCaptor.getValue();
    assertThat(value, is(notNullValue()));
    assertThat(value.getActionType(), is(equalTo(ActionType.DELETED)));
    assertThat(value.getPerformedBy(), is(equalTo("testUser")));
  }
}
