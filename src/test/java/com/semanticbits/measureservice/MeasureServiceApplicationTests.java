package com.semanticbits.measureservice;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.semanticbits.measureservice.resources.MeasureController;

@SpringBootTest
class MeasureServiceApplicationTests {
  @Autowired private MeasureController controller;

  @Test
  /*
   * Note the use of @SpringBootTest annotation which tells Spring Boot to look
   * for a main configuration class (one with @SpringBootApplication, for
   * instance) and use that to start a Spring application context. Empty
   * contextLoads() is a test to verify if the application is able to load Spring
   * context successfully or not.
   * https://stackoverflow.com/questions/49887939/what-is-the-use-of-contextloads-
   * method-in-spring-boot-junit-testcases
   */
  void contextLoads() {
    // if the Spring App loads, this will pass
    assertNotNull(controller);
  }
}
