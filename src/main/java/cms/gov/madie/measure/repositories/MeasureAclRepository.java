package cms.gov.madie.measure.repositories;

import gov.cms.madie.models.measure.Measure;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MeasureAclRepository {
  /**
   * Measure is considered to be my measure if provided user is the owner of this measure or is
   * shared with provided user and measure is active(measure.active = true)
   *
   * @param userId- current user
   * @param pageable- instance of Pageable
   * @param searchTerm- additional search term
   * @return Pageable List of measures
   */
  Page<Measure> findMyActiveMeasures(String userId, Pageable pageable, String searchTerm);
}
