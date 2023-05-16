package gov.cms.madie.measure.utilities.qicore411;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import cms.gov.madie.measure.exceptions.InternalServerException;
import cms.gov.madie.measure.utils.PackagingUtility;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.library.CqlLibrary;
import gov.cms.madie.models.measure.Export;
import gov.cms.madie.packaging.utils.ZipUtility;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipOutputStream;
import java.util.HashMap;

@Slf4j
public class PackagingUtilityImpl implements PackagingUtility {
  private FhirContext context;

  public PackagingUtilityImpl() {
    context = FhirContext.forR4();
  }

  private static final String TEXT_CQL = "text/cql";
  private static final String CQL_DIRECTORY = "cql/";
  private static final String RESOURCES_DIRECTORY = "resources/";

  @Override
  public byte[] getZipBundle(Export export, String exportFileName) throws InternalServerException {
    // TODO Auto-generated method stub
    String measureBundle = export.getMeasureBundleJson();
    IParser jsonParser = context.newJsonParser();
    IParser xmlParser = context.newXmlParser();
    org.hl7.fhir.r4.model.Bundle bundle =
        (org.hl7.fhir.r4.model.Bundle) jsonParser.parseResource(measureBundle);
    if (bundle == null) {
      return null;
    }
    org.hl7.fhir.r4.model.DomainResource measure =
        (org.hl7.fhir.r4.model.DomainResource) ResourceUtils.getResource(bundle, "Measure");
    if (measure == null) {
      throw new InternalServerException("Measure is Null");
    }
    String humanReadable = measure.getText().getDivAsString();

    String template = ResourceUtils.getData("/templates/HumanReadable.liquid");
    String humanReadableWithCSS = template.replace("human_readable_content_holder", humanReadable);

    byte[] result = zipEntries(exportFileName, jsonParser, xmlParser, bundle, humanReadableWithCSS);
    return result;
  }

  private byte[] zipEntries(
      String exportFileName,
      IParser jsonParser,
      IParser xmlParser,
      org.hl7.fhir.r4.model.Bundle bundle,
      String humanReadableWithCSS) {

    Map<String, byte[]> entries = new HashMap<String, byte[]>();
    try {

      // Add Json
      byte[] jsonBytes = jsonParser.setPrettyPrint(true).encodeResourceToString(bundle).getBytes();
      entries.put(exportFileName + ".json", jsonBytes);

      // Add Xml
      byte[] xmlBytes = xmlParser.setPrettyPrint(true).encodeResourceToString(bundle).getBytes();
      entries.put(exportFileName + ".xml", xmlBytes);

      // add Library Cql Files to Export
      List<CqlLibrary> cqlLibraries = getCQLForLibraries(bundle);
      for (CqlLibrary library : cqlLibraries) {
        String filePath =
            CQL_DIRECTORY + library.getCqlLibraryName() + "-" + library.getVersion() + ".cql";
        String data = library.getCql();

        entries.put(filePath, xmlBytes);
      }
      // add Library Resources to Export
      List<Library> libraries = getLibraryResources(bundle);
      for (Library library1 : libraries) {
        String json = jsonParser.setPrettyPrint(true).encodeResourceToString(library1);
        String xml = xmlParser.setPrettyPrint(true).encodeResourceToString(library1);
        String fileName = RESOURCES_DIRECTORY + library1.getName() + "-" + library1.getVersion();
        entries.put(fileName + ".json", json.getBytes());
        entries.put(fileName + ".xml", xml.getBytes());
      }

      entries.put(exportFileName + ".html", humanReadableWithCSS.getBytes());
    } catch (InternalServerException ex) {
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] zipFileBytes = new ZipUtility(baos, new ZipOutputStream(baos)).zipEntries(entries);
    return zipFileBytes;
  }

  private List<CqlLibrary> getCQLForLibraries(Bundle measureBundle) {
    List<Library> libraries = getLibraryResources(measureBundle);
    List<CqlLibrary> cqlLibries = new ArrayList<>();
    for (Library library : libraries) {
      Attachment attachment = getCqlAttachment(library);
      String cql = new String(attachment.getData());
      cqlLibries.add(
          CqlLibrary.builder()
              .cqlLibraryName(library.getName())
              .cql(cql)
              .version(Version.parse(library.getVersion()))
              .build());
    }
    return cqlLibries;
  }

  private Attachment getCqlAttachment(Library library) {
    return library.getContent().stream()
        .filter(content -> StringUtils.equals(TEXT_CQL, content.getContentType()))
        .findAny()
        .orElse(null);
  }

  private List<Library> getLibraryResources(Bundle measureBundle) {
    return measureBundle.getEntry().stream()
        .filter(
            entry -> StringUtils.equals("Library", entry.getResource().getResourceType().name()))
        .map(entry -> (Library) entry.getResource())
        .toList();
  }
}
