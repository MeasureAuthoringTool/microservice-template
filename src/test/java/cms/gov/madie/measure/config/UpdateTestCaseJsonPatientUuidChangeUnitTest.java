package cms.gov.madie.measure.config;

import cms.gov.madie.measure.repositories.MeasureRepository;
import cms.gov.madie.measure.services.TestCaseService;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.TestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(MockitoExtension.class)
class UpdateTestCaseJsonPatientUuidChangeUnitTest {

    final String json = "{\"resourceType\":\"Bundle\",\"id\":\"2106\",\"meta\":{\"versionId\":\"1\",\"lastUpdated\":\"2022-09-06T20:47:21.183+00:00\"},\"type\":\"collection\",\"entry\":[{\"fullUrl\":\"http://local/Encounter/2\",\"resource\":{\"id\":\"2\",\"resourceType\":\"Encounter\",\"meta\":{\"versionId\":\"1\",\"lastUpdated\":\"2021-10-13T03:34:10.160+00:00\",\"source\":\"#nEcAkGd8PRwPP5fA\"},\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">Sep 9th 2021 for Asthma<a name=\\\"mm\\\"/></div>\"},\"class\":{\"system\":\"http://terminology.hl7.org/CodeSystem/v3-ActCode\",\"code\":\"IMP\",\"display\":\"inpatient encounter\"},\"status\":\"planned\",\"type\":[{\"text\":\"OutPatient\"}],\"subject\":{\"reference\":\"Patient/1\"},\"participant\":[{\"individual\":{\"reference\":\"Practitioner/30164\",\"display\":\"Dr John Doe\"}}],\"period\":{\"start\":\"2023-08-10T03:34:10.054Z\",\"end\":\"2023-08-15T03:34:10.054Z\"}}},{\"fullUrl\":\"http://local/Encounter/3\",\"resource\":{\"id\":\"3\",\"resourceType\":\"Encounter\",\"meta\":{\"versionId\":\"1\",\"lastUpdated\":\"2021-10-13T03:34:10.160+00:00\",\"source\":\"#nEcAkGd8PRwPP5fA\"},\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">Sep 9th 2021 for Asthma<a name=\\\"mm\\\"/></div>\"},\"class\":{\"system\":\"http://terminology.hl7.org/CodeSystem/v3-ActCode\",\"code\":\"IMP\",\"display\":\"inpatient encounter\"},\"status\":\"finished\",\"type\":[{\"text\":\"OutPatient\"}],\"subject\":{\"reference\":\"Patient/1\"},\"participant\":[{\"individual\":{\"reference\":\"Practitioner/30164\",\"display\":\"Dr John Doe\"}}],\"period\":{\"start\":\"2023-09-12T03:34:10.054Z\",\"end\":\"2023-09-13T09:34:10.054Z\"}}},{\"fullUrl\":\"http://local/Patient/1\",\"resource\":{\"id\":\"1\",\"resourceType\":\"Patient\",\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">Lizzy Health</div>\"},\"meta\":{\"profile\":\"http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-patient\"},\"identifier\":[{\"system\":\"http://clinfhir.com/fhir/NamingSystem/identifier\",\"value\":\"20181011LizzyHealth\"}],\"name\":[{\"use\":\"official\",\"text\":\"Lizzy Health\",\"family\":\"Health\",\"given\":[\"Lizzy\"]}],\"gender\":\"female\",\"birthDate\":\"2000-10-11\"}}]}";
    final String json2 = "{ \"resourceType\": \"Bundle\", \"id\": \"NUMERFail-SBP139DBP89TakenDuringEMER\", \"type\": \"collection\", \"entry\": [ { \"fullUrl\": \"633c9d020968f8012250fc60\", \"resource\": { \"resourceType\": \"Patient\", \"id\": \"Patient-7\", \"meta\": { \"profile\": [ \"http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-patient\" ] }, \"extension\": [ { \"url\": \"http://hl7.org/fhir/us/core/StructureDefinition/us-core-race\", \"extension\": [ { \"url\": \"ombCategory\", \"valueCoding\": { \"code\": \"2028-9\", \"system\": \"urn:oid:2.16.840.1.113883.6.238\", \"display\": \"Asian\" } }, { \"url\": \"text\", \"valueString\": \"Asian\" } ] }, { \"url\": \"http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity\", \"extension\": [ { \"url\": \"ombCategory\", \"valueCoding\": { \"code\": \"2135-2\", \"system\": \"urn:oid:2.16.840.1.113883.6.238\", \"display\": \"Hispanic or Latino\" } }, { \"url\": \"text\", \"valueString\": \"Hispanic or Latino\" } ] } ], \"identifier\": [ { \"system\": \"http://hospital.smarthealthit.org\", \"value\": \"999999995\" } ], \"name\": [ { \"family\": \"Bertha\", \"given\": [ \"Betty\" ] } ], \"birthDate\": \"2005-12-31\", \"gender\": \"female\" } }, { \"fullUrl\": \"633c9d020968f8012250fc61\", \"resource\": { \"resourceType\": \"Encounter\", \"id\": \"Encounter-7\", \"meta\": { \"profile\": [ \"http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-encounter\" ] }, \"subject\": { \"reference\": \"Patient/Patient-7\" }, \"status\": \"finished\", \"class\": { \"code\": \"AMB\", \"system\": \"http://terminology.hl7.org/CodeSystem/v3-ActCode\", \"display\": \"ambulatory\" }, \"type\": [ { \"coding\": [ { \"code\": \"3391000175108\", \"system\": \"http://snomed.info/sct\", \"display\": \"Office visit for pediatric care and assessment (procedure)\" } ] } ], \"period\": { \"start\": \"2024-01-01T00:00:00.000Z\", \"end\": \"2024-01-01T01:00:00.000Z\" } } }, { \"fullUrl\": \"633c9d020968f8012250fc62\", \"resource\": { \"resourceType\": \"Condition\", \"id\": \"Condition-7\", \"meta\": { \"profile\": [ \"http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-condition\" ] }, \"category\": [ { \"coding\": [ { \"code\": \"problem-list-item\", \"system\": \"http://terminology.hl7.org/CodeSystem/condition-category\", \"display\": \"Problem List Item\" } ] } ], \"subject\": { \"reference\": \"Patient/Patient-7\" }, \"code\": { \"coding\": [ { \"code\": \"371125006\", \"system\": \"http://snomed.info/sct\", \"display\": \"Labile essential hypertension (disorder)\" } ] }, \"onsetDateTime\": \"2024-06-30T23:59:59.000Z\", \"clinicalStatus\": { \"coding\": [ { \"code\": \"active\", \"system\": \"http://terminology.hl7.org/CodeSystem/condition-clinical\" } ] } } }, { \"fullUrl\": \"633c9d020968f8012250fc63\", \"resource\": { \"resourceType\": \"Observation\", \"id\": \"Observation-7\", \"meta\": { \"profile\": [ \"http://hl7.org/fhir/StructureDefinition/bp\" ] }, \"subject\": { \"reference\": \"Patient/Patient-7\" }, \"category\": [ { \"coding\": [ { \"code\": \"vital-signs\", \"system\": \"http://terminology.hl7.org/CodeSystem/observation-category\", \"display\": \"Vital Signs\" } ], \"text\": \"Vital Signs\" } ], \"code\": { \"coding\": [ { \"code\": \"85354-9\", \"system\": \"http://loinc.org\", \"display\": \"Blood pressure panel with all children optional\" } ] }, \"component\": [ { \"code\": { \"coding\": [ { \"code\": \"8480-6\", \"system\": \"http://loinc.org\", \"display\": \"Systolic blood pressure\" } ] }, \"valueQuantity\": { \"value\": 139, \"code\": \"mm[Hg]\", \"system\": \"http://unitsofmeasure.org\", \"unit\": \"mmHg\" } }, { \"code\": { \"coding\": [ { \"code\": \"8462-4\", \"system\": \"http://loinc.org\", \"display\": \"Diastolic blood pressure\" } ] }, \"valueQuantity\": { \"value\": 89, \"code\": \"mm[Hg]\", \"system\": \"http://unitsofmeasure.org\", \"unit\": \"mmHg\" } } ], \"status\": \"final\", \"effectiveDateTime\": \"2024-01-01T00:00:00.000Z\", \"encounter\": { \"reference\": \"Encounter/Encounter-7-1\" } } }, { \"fullUrl\": \"633c9d020968f8012250fc64\", \"resource\": { \"resourceType\": \"Encounter\", \"id\": \"Encounter-7-1\", \"meta\": { \"profile\": [ \"http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-encounter\" ] }, \"subject\": { \"reference\": \"Patient/Patient-7\" }, \"status\": \"finished\", \"class\": { \"code\": \"EMER\", \"system\": \"http://terminology.hl7.org/CodeSystem/v3-ActCode\", \"display\": \"emergency\" }, \"type\": [ { \"coding\": [ { \"code\": \"4525004\", \"system\": \"http://snomed.info/sct\", \"display\": \"Emergency department patient visit (procedure)\" } ] } ], \"period\": { \"start\": \"2024-01-01T00:00:00.000Z\", \"end\": \"2024-01-01T01:00:00.000Z\" } } } ] } ";


    @Mock
    MeasureRepository measureRepository;

    @Mock
    TestCaseService testCaseService;

    @InjectMocks UpdateTestCaseJsonPatientUuidChangeUnit changeUnit;

    Measure measure1;
    Measure measure2;

    TestCase tc1;
    TestCase tc2;

    @Captor
    private ArgumentCaptor<Measure> measureArgumentCaptor1;
    @Captor private ArgumentCaptor<Measure> measureArgumentCaptor2;


    @BeforeEach
    public void setup() {
        tc1 = TestCase.builder().name("TC1").patientId(UUID.randomUUID()).json(json).build();
        tc2 = TestCase.builder().name("TC2").patientId(UUID.randomUUID()).json(json2).build();

        measure1 = Measure.builder()
                .measureName("Measure1")
                .model(ModelType.QI_CORE.getValue())
                .testCases(List.of(tc1, tc2))
                .build();

        measure2 = Measure
                .builder()
                .measureName("QdmMeasure")
                .model(ModelType.QDM_5_6.getValue())
                .build();
    }

    @Test
    public void testChangeUnitExecutionEmptyRepository() {
        // given
        when(measureRepository.findAll()).thenReturn(List.of());

        // when
        changeUnit.updatedTestCaseJsonWithPatientUuid(measureRepository, testCaseService);

        // then
        verifyNoMoreInteractions(measureRepository);
        verifyNoInteractions(testCaseService);
    }

    @Test
    public void testChangeUnitExecution() {
        // given
        when(measureRepository.findAll()).thenReturn(List.of(measure1));

        // when
        changeUnit.updatedTestCaseJsonWithPatientUuid(measureRepository, testCaseService);

        // then
        verify(measureRepository, times(1)).save(measureArgumentCaptor1.capture());
        Measure measure = measureArgumentCaptor1.getValue();
        assertThat(measure.getTestCases(), is(notNullValue()));
        assertThat(measure.getTestCases().size(), is(equalTo(2)));

    }

}