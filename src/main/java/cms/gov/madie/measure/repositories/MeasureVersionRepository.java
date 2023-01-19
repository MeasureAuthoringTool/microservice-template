package cms.gov.madie.measure.repositories;

import java.util.Optional;

import gov.cms.madie.models.common.Version;

public interface MeasureVersionRepository {

  Optional<Version> findMaxVersionByMeasureSetId(String measureSetId);

  Optional<Version> findMaxMinorVersionByMeasureSetIdAndVersionMajor(
      String measureSetId, int majorVersion);

  Optional<Version> findMaxRevisionNumberByMeasureSetIdAndVersionMajorAndMinor(
      String measureSetId, int majorVersion, int minorVersion);
}
