package cms.gov.madie.measure.repositories;

import cms.gov.madie.measure.dto.MeasureListDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MeasureAclRepository {
  /**
   * Measure is considered to be my measure if provided user is the owner of this measure or is
   * shared with provided user and measure is active(measure.active = true)
   *
   * @param userId- current user
   * @param pageable- instance of Pageable
   * @return Pageable List of measures
   */
  Page<MeasureListDTO> findMyActiveMeasures(String userId, Pageable pageable, String searchTerm);
}
