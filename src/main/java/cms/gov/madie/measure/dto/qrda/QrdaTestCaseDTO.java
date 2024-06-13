package cms.gov.madie.measure.dto.qrda;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class QrdaTestCaseDTO {
  String testCaseId;
  String lastName;
  String firstName;
  PopulationDTO[] populations;
  QrdaGroupStratificationDTO[] stratifications;
}
