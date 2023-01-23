package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.services.VirusScanClient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.security.Principal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/scan")
@AllArgsConstructor
public class ScanController {

  private VirusScanClient virusScanClient;

  @PostMapping("/file")
  public ResponseEntity<VirusScanClient.ScanValidationDto> importTestCases(
      @RequestParam("file") MultipartFile multipartFile,
      Principal principal) {
    log.info("DOING THE THING HERE");
    String fileName = StringUtils.cleanPath(multipartFile.getOriginalFilename());

    VirusScanClient.VirusScanResponseDto scanResponse = virusScanClient.scanFile(multipartFile.getResource());
    if (scanResponse.getFilesScanned() > 0 && scanResponse.getFilesScanned() == scanResponse.getCleanFileCount()) {
      // all files are clean!
      return ResponseEntity.ok(VirusScanClient.ScanValidationDto.builder().valid(true).build());
    } else {
      // errors occurred or virus detected
      return ResponseEntity.ok(
          VirusScanClient.ScanValidationDto.builder()
              .valid(false)
              .error(new ObjectError(fileName, "File validation failed with error code V100."))
              .build()
      );
    }
  }

}
