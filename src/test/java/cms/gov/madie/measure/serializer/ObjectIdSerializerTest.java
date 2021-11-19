package cms.gov.madie.measure.serializer;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonGenerator;

@ExtendWith(MockitoExtension.class)
class ObjectIdSerializerTest {

  @Mock JsonGenerator jsonGen;

  @InjectMocks private ObjectIdSerializer serializer;

  @Test
  void test() {

    // ObjectId value, JsonGenerator jsonGen, SerializerProvider provider
    ObjectId id = ObjectId.get();

    try {
      serializer.serialize(id, jsonGen, null);
    } catch (IOException e) {
      fail();
    }

    try {
      verify(jsonGen, times(1)).writeString(ArgumentMatchers.<String>any());
    } catch (IOException e1) {
      fail();
    }
  }
}
