package cms.gov.madie.measure.utils;

import cms.gov.madie.measure.repositories.GeneratorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
public class SequenceGeneratorUtilTest {
  @Mock private GeneratorRepository generatorRepository;
  @InjectMocks private SequenceGeneratorUtil sequenceGeneratorUtil;

  @Test
  public void testGenerateSequenceNumber() {
    doReturn(1).when(generatorRepository).findAndModify(anyString());

    int output = sequenceGeneratorUtil.generateSequenceNumber("cms_id");
    assertThat(output, is(notNullValue()));
    assertThat(output, is(1));
  }
}
