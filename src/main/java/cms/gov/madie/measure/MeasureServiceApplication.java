package cms.gov.madie.measure;

import cms.gov.madie.measure.services.LogInterceptor;
import io.mongock.driver.api.driver.ConnectionDriver;
import io.mongock.driver.mongodb.springdata.v3.SpringDataMongoV3Driver;
import io.mongock.runner.springboot.EnableMongock;
import io.mongock.runner.springboot.MongockSpringboot;
import io.mongock.runner.springboot.base.MongockInitializingBeanRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Using @EnableMongock with minimal configuration only requires changeLog package to scan in
 * property file
 */
@SpringBootApplication
@EnableMongock
@EnableCaching
@EnableScheduling
public class MeasureServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(MeasureServiceApplication.class, args);
  }
  //    @Bean
  //  public MongockInitializingBeanRunner mongockApplicationRunner(ApplicationContext
  // springContext, MongoTemplate mongoTemplate) {
  //    SpringDataMongoV3Driver driver = SpringDataMongoV3Driver.withDefaultLock(mongoTemplate);
  //    return MongockSpringboot.builder()
  //        .setDriver(driver)
  //        .addMigrationScanPackage("cms.gov.madie.measure.config")
  //        .setSpringContext(springContext)
  //        // any extra configuration you need
  //        .buildInitializingBeanRunner();
  //  }

  @Bean
  public WebMvcConfigurer corsConfigurer(@Autowired LogInterceptor logInterceptor) {
    return new WebMvcConfigurer() {

      @Override
      public void addInterceptors(InterceptorRegistry registry) {
        WebMvcConfigurer.super.addInterceptors(registry);
        registry.addInterceptor(logInterceptor);
      }

      @Override
      public void addCorsMappings(CorsRegistry registry) {
        registry
            .addMapping("/**")
            .allowedMethods("PUT", "POST", "GET", "DELETE")
            .allowedOrigins(
                "http://localhost:9000",
                "https://dev-madie.hcqis.org",
                "https://test-madie.hcqis.org",
                "https://impl-madie.hcqis.org");
      }
    };
  }
}
