package cms.gov.madie.measure.services;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import cms.gov.madie.measure.utils.ResourceUtility;

import java.util.HashMap;

public class ResourceUtilityFactory {

  public static ResourceUtility getInstance(String model)
      throws InstantiationException, IllegalAccessException, IllegalArgumentException,
          InvocationTargetException, NoSuchMethodException, SecurityException,
          ClassNotFoundException {
    Map<String, String> modelMap =
        new HashMap<>() {
          {
            put("QI-Core v4.1.1", "qicore411");
          }
        };

    String className =
        "gov.cms.madie.measure.utilities." + modelMap.get(model) + ".ResourceUtilityImpl";
    ResourceUtility newObject =
        (ResourceUtility) Class.forName(className).getConstructor().newInstance();
    return newObject;
  }
}
