package cms.gov.madie.measure.models;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

@Getter
public enum ModelType {
  QI_CORE("1", "QI-Core");

  private String key;
  private String value;
  private static final Map<String, ModelType> MODEL_TYPE_BY_VALUE =
      new HashMap<String, ModelType>();

  static {
    for (ModelType mt : values()) {
      System.out.println("mt.getValue() = " + mt.getValue());
      MODEL_TYPE_BY_VALUE.put(mt.getValue(), mt);
    }
  }

  ModelType(String key, String value) {
    System.out.println("key = " + key + " value = " + value);
    this.key = key;
    this.value = value;
  }

  @Override
  public String toString() {
    return this.getValue();
  }

  public static ModelType valueOfName(final String name) {
    return MODEL_TYPE_BY_VALUE.get(name);
  }
}
