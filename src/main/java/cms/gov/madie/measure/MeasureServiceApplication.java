package cms.gov.madie.measure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class MeasureServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(MeasureServiceApplication.class, args);
  }

  @Bean
  public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
	registry.addMapping("/**").allowedMethods("PUT", "POST", "GET").allowedOrigins(
	    "http://localhost:9000", "https://dev-madie.hcqis.org", "https://test-madie.hcqis.org",
	    "https://impl-madie.hcqis.org");
      }
    };
  }
}
