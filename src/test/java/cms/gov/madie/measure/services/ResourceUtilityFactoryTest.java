package cms.gov.madie.measure.services;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;

import cms.gov.madie.measure.utils.PackagingUtility;

class ResourceUtilityFactoryTest {

  @Test
  void testGetInstance_fails() {
    try {
      PackagingUtility utility = PackagingUtilityFactory.getInstance("QI-Core");
      fail("Should not be set " + utility.toString());

    } catch (InstantiationException
        | IllegalAccessException
        | IllegalArgumentException
        | InvocationTargetException
        | NoSuchMethodException
        | SecurityException
        | ClassNotFoundException e) {
      assertTrue(e instanceof ClassNotFoundException);
    }
  }

  @Test
  void testGetInstance() {
    try {
      PackagingUtility utility = PackagingUtilityFactory.getInstance("QI-Core v4.1.1");
      assertNotNull(utility);

    } catch (InstantiationException
        | IllegalAccessException
        | IllegalArgumentException
        | InvocationTargetException
        | NoSuchMethodException
        | SecurityException
        | ClassNotFoundException e) {
      fail(e);
    }
  }
}
