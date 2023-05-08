package gov.cms.madie.measure.utilities.qicore411;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import cms.gov.madie.measure.exceptions.InternalServerException;
import cms.gov.madie.measure.utils.ResourceUtility;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.library.CqlLibrary;
import gov.cms.madie.models.measure.Export;

import java.util.ArrayList;
import java.util.List;

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ResourceUtilityImpl implements ResourceUtility {
  private FhirContext context;

  public ResourceUtilityImpl() {
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
    var measureEntry =
        bundle.getEntry().stream()
            .filter(
                entry ->
                    StringUtils.equalsIgnoreCase(
                        "Measure", entry.getResource().getResourceType().toString()))
            .findFirst();
    org.hl7.fhir.r4.model.DomainResource measure =
        (org.hl7.fhir.r4.model.DomainResource)
            measureEntry
                .map(org.hl7.fhir.r4.model.Bundle.BundleEntryComponent::getResource)
                .orElse(null);
    if (measure == null) {
      throw new InternalServerException("Measure is Null");
    }
    String humanReadable = measure.getText().getDivAsString();

    String template = ResourceUtils.getData("/templates/HumanReadable.liquid");
    String humanReadableWithCSS = template.replace("human_readable_content_holder", humanReadable);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    zipEntries(exportFileName, jsonParser, xmlParser, bundle, humanReadableWithCSS, baos);
    return baos.toByteArray();
  }

  private void zipEntries(
      String exportFileName,
      IParser jsonParser,
      IParser xmlParser,
      org.hl7.fhir.r4.model.Bundle bundle,
      String humanReadableWithCSS,
      ByteArrayOutputStream baos) {
    ZipOutputStream zos = new ZipOutputStream(baos);
    try {

      // Add Json
      byte[] jsonBytes = jsonParser.setPrettyPrint(true).encodeResourceToString(bundle).getBytes();
      ZipEntry entry = new ZipEntry(exportFileName + ".json");
      entry.setSize(jsonBytes.length);
      zos.putNextEntry(entry);
      zos.write(jsonBytes);
      zos.closeEntry();
      // Add Xml
      byte[] xmlBytes = xmlParser.setPrettyPrint(true).encodeResourceToString(bundle).getBytes();
      entry = new ZipEntry(exportFileName + ".xml");
      entry.setSize(xmlBytes.length);
      zos.putNextEntry(entry);
      zos.write(xmlBytes);
      zos.closeEntry();
      // add Library Cql Files to Export
      List<CqlLibrary> cqlLibraries = getCQLForLibraries(bundle);
      for (CqlLibrary library : cqlLibraries) {
        String filePath =
            CQL_DIRECTORY + library.getCqlLibraryName() + "-" + library.getVersion() + ".cql";
        String data = library.getCql();
        entry = new ZipEntry(filePath);
        entry.setSize(data.getBytes().length);
        zos.putNextEntry(entry);
        zos.write(data.getBytes());
        zos.closeEntry();
      }
      // add Library Resources to Export
      addLibraryResources(jsonParser, xmlParser, bundle, zos);

      entry = new ZipEntry(exportFileName + ".html");
      entry.setSize(humanReadableWithCSS.getBytes().length);
      zos.putNextEntry(entry);
      zos.write(humanReadableWithCSS.getBytes());
      zos.closeEntry();
    } catch (InternalServerException | IOException ex) {
      // TODO Auto-generated catch block
      ex.printStackTrace();
    } finally {
      try {
        zos.close();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  private void addLibraryResources(
      IParser jsonParser,
      IParser xmlParser,
      org.hl7.fhir.r4.model.Bundle bundle,
      ZipOutputStream zos)
      throws IOException {
    ZipEntry entry;
    List<Library> libraries = getLibraryResources(bundle);
    for (Library library : libraries) {
      String json = jsonParser.setPrettyPrint(true).encodeResourceToString(library);
      String xml = xmlParser.setPrettyPrint(true).encodeResourceToString(library);

      String fileName = RESOURCES_DIRECTORY + library.getName() + "-" + library.getVersion();
      entry = new ZipEntry(fileName + ".json");
      entry.setSize(json.getBytes().length);
      zos.putNextEntry(entry);
      zos.write(json.getBytes());
      zos.closeEntry();
      entry = new ZipEntry(fileName + ".xml");
      entry.setSize(xml.getBytes().length);
      zos.putNextEntry(entry);
      zos.write(xml.getBytes());
      zos.closeEntry();
    }
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
