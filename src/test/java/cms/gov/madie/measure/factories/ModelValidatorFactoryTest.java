package cms.gov.madie.measure.factories;

import cms.gov.madie.measure.exceptions.UnsupportedTypeException;
import cms.gov.madie.measure.services.ModelValidator;
import cms.gov.madie.measure.services.QdmModelValidator;
import cms.gov.madie.measure.services.QiCoreModelValidator;
import cms.gov.madie.measure.services.ServiceConstants;
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
class ModelValidatorFactoryTest {

    @Mock
    QdmModelValidator qdmModelValidator;

    @Mock
    QiCoreModelValidator qiCoreModelValidator;

    ModelValidatorFactory modelValidatorFactory;

    @BeforeEach
    void setup() {
        modelValidatorFactory = new ModelValidatorFactory(Map.of(ServiceConstants.QDM_VALIDATOR, qdmModelValidator, ServiceConstants.QICORE_VALIDATOR, qiCoreModelValidator));
    }

    @Test
    void testFactoryReturnsQdmValidator() {
        ModelValidator output = modelValidatorFactory.getModelValidator(ModelType.QDM_5_6);
        assertThat(output, is(equalTo(qdmModelValidator)));
    }

    @Test
    void testFactoryReturnsQiCoreValidator() {
        ModelValidator output = modelValidatorFactory.getModelValidator(ModelType.QI_CORE);
        assertThat(output, is(equalTo(qiCoreModelValidator)));
    }

    @Test
    void testFactoryThrowsException() {
        ModelValidatorFactory factory = new ModelValidatorFactory(Map.of(ServiceConstants.QDM_VALIDATOR, qdmModelValidator));
        assertThrows(UnsupportedTypeException.class, () -> factory.getModelValidator(ModelType.QI_CORE));
    }

}