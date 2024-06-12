package cms.gov.madie.measure.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class DELETETHIS implements HandlerInterceptor {

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType("text/html; charset=utf-8");
    response
        .getWriter()
        .write(
            "<html><head><title>Request Rejected</title></head><body>The requested URL was rejected by CMS security policy. "
                + "If you believe this is in error, please contact the HIDS Security Operations Center "
                + "<a href=\"mailto:soc@hcqis.org\">soc@hcqis.org</a> and reference your Support ID:  "
                + "16683182497183476164<br><br><a href='javascript:history.back();'>[Go Back]</a></body></html>");
    response.getWriter().flush();
    return false;
  }
}
