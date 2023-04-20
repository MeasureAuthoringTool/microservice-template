package cms.gov.madie.measure.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.time.LocalDateTime;

@Slf4j(topic = "action_audit")
@Order(value = Ordered.HIGHEST_PRECEDENCE)
@Component
public class LogInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    final String username =
        request.getUserPrincipal() == null ? "" : request.getUserPrincipal().getName();
    log.info(
        "[{}] User [{}] calling [{}] on path [{}].",
        LocalDateTime.now(),
        username,
        request.getMethod(),
        request.getRequestURI());
    return HandlerInterceptor.super.preHandle(request, response, handler);
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    final String username =
        request.getUserPrincipal() == null ? "" : request.getUserPrincipal().getName();
    log.info(
        "User [{}] called [{}] on path [{}] and got response code [{}]",
        username,
        request.getMethod(),
        request.getRequestURI(),
        response.getStatus());
  }
}
