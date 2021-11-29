package cms.gov.madie.measure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class MeasureServiceApplication {

  @Autowired private Environment env;

  public static void main(String[] args) {
    SpringApplication.run(MeasureServiceApplication.class, args);
  }

  @Configuration
  @EnableWebMvc
  public class WebConfig implements WebMvcConfigurer {

    @Value("${madie.allowedApi}")
    private String myAllowedApi;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
      registry
          .addMapping("/api/**")
          .allowedOrigins("http://localhost:9000")
          .allowedMethods("GET", "POST", "PUT", "DELETE")
          .allowedOrigins(myAllowedApi);
    }
  }
}
