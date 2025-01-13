package cms.gov.madie.measure.repositories;

import cms.gov.madie.measure.dto.MeasureListDTO;

import cms.gov.madie.measure.dto.MeasureSearchCriteria;
import gov.cms.madie.models.dto.LibraryUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MeasureAclRepository {
  /**
   * Measure is considered to be my measure if provided user is the owner of this measure or is
   * shared with provided user and measure is active(measure.active = true)
   *
   * @param userId- current user
   * @param pageable- instance of Pageable
   * @return Pageable List of measures
   */
  Page<MeasureListDTO> findActiveMeasures(
      String userId,
      Pageable pageable,
      MeasureSearchCriteria searchCriteria,
      boolean filterByCurrentUser);

  /**
   * Get all the measures(name, version and owner) if they include any version of given library name
   *
   * @param name -> library name for which usage needs to be determined
   * @return List<LibraryUsage> -> LibraryUsage: name, version and owner of including library
   */
  List<LibraryUsage> findLibraryUsageByLibraryName(String name);
}
