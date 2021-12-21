package cms.gov.madie.measure;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.mongock.runner.springboot.EnableMongock;

@Profile("!test")
@EnableMongock
@Configuration
public class MongockConfig {}
