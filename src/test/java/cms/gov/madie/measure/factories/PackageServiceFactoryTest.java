package cms.gov.madie.measure.factories;

import cms.gov.madie.measure.exceptions.UnsupportedTypeException;
import cms.gov.madie.measure.services.PackageService;
import cms.gov.madie.measure.services.QdmPackageService;
import cms.gov.madie.measure.services.QicorePackageService;
import gov.cms.madie.models.common.ModelType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class PackageServiceFactoryTest {

  @Mock QdmPackageService qdmPackageService;

  @Mock QicorePackageService qicorePackageService;

  PackageServiceFactory packageServiceFactory;

  @BeforeEach
  void setup() {
    packageServiceFactory =
        new PackageServiceFactory(
            Map.of(
                "qdmPackageService",
                qdmPackageService,
                "qicorePackageService",
                qicorePackageService));
  }

  @Test
  void testFactoryReturnsQdmPackageService() {
    PackageService output = packageServiceFactory.getPackageService(ModelType.QDM_5_6);
    assertThat(output, is(equalTo(qdmPackageService)));
  }

  @Test
  void testFactoryReturnsQiCorePackageService() {
    PackageService output = packageServiceFactory.getPackageService(ModelType.QI_CORE);
    assertThat(output, is(equalTo(qicorePackageService)));
  }

  @Test
  void testFactoryThrowsException() {
    PackageServiceFactory factory = new PackageServiceFactory(Map.of("qdm", qdmPackageService));
    assertThrows(
        UnsupportedTypeException.class, () -> factory.getPackageService(ModelType.QI_CORE));
  }
}
