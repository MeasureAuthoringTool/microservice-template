package cms.gov.madie.measure.resources;

import cms.gov.madie.measure.models.UmlsApiKey;
import cms.gov.madie.measure.repositories.UmlsKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UmlsApiKeyController {
  private UmlsKeyRepository repository;

  @Autowired
  public UmlsApiKeyController(UmlsKeyRepository repository) {
    this.repository = repository;
  }

  @PostMapping("/key")
  public ResponseEntity<Void> saveKey(@RequestBody String key) {
    UmlsApiKey newKey =
        UmlsApiKey.builder()
            .apiKey(key)
            .harpId("me")
            .build();
    repository.save(newKey);
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @GetMapping("/key/{harpId}")
  public ResponseEntity<UmlsApiKey> fetchKeyByHarpId(@PathVariable("harpId") String harpId) {
    Optional<UmlsApiKey> key = repository.findByHarpId(harpId);
    return key.map(ResponseEntity::ok).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }
}
