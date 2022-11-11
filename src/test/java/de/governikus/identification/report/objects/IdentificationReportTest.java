package de.governikus.identification.report.objects;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.governikus.identification.report.constants.SchemaConstants;
import de.governikus.identification.report.objects.subjects.EidCardPersonRef;
import de.governikus.identification.report.objects.subjects.SubjectRef;
import de.governikus.identification.report.setup.FileReferences;
import de.governikus.identification.report.validation.SchemaValidator;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.OutputUnit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 05.09.2022
 */
@Slf4j
public class IdentificationReportTest implements FileReferences
{

  /**
   * validates identification reports with an empty subject and with two different types of subjects
   */
  @SneakyThrows
  @ParameterizedTest
  @ValueSource(strings = {IDENTIFICATION_REPORT_2_0, IDENTIFICATION_REPORT_WITH_EID_CARD_SUBJECT_2_0})
  public void testVerifyWithAndWithoutSubjects(String reportLocation)
  {
    JsonObject resource = new JsonObject(readResourceFile(reportLocation));
    Assertions.assertTrue(SchemaValidator.isJsonValid(SchemaConstants.Locations.IDENTIFICATION_REPORT_SCHEMA_LOCATION,
                                                      resource));
  }

  /**
   * this test shows that the validation fails if the required fields are missing
   */
  @Test
  public void testRequiredFieldsAreMissing()
  {
    IdentificationReport<EidCardPersonRef> identificationReport = IdentificationReport.<EidCardPersonRef> builder()
                                                                                      .build();

    JsonObject resource = identificationReport.toJson();

    OutputUnit result = SchemaValidator.validateJsonObject(SchemaConstants.Locations.IDENTIFICATION_REPORT_SCHEMA_LOCATION,
                                                           resource);

    Assertions.assertFalse(result.getValid(), result.getError());
    Assertions.assertEquals(5, result.getErrors().size());
    for ( OutputUnit error : result.getErrors() )
    {
      Assertions.assertEquals("required", error.getKeyword());
    }
  }

  /**
   * this test makes sure that the validation will fail if a customized property was added within the document
   * references
   */
  @Test
  public void testCustomPropertyInDocumentReferenceAdded()
  {
    // @formatter:off
    JsonObject documentReference = JsonObject.of("documentId", UUID.randomUUID().toString(),
                                                 "documentName", "test.pdf",
                                                 "customField", "unwanted property");
    // @formatter:on

    List<JsonObject> documentReferenceList = new ArrayList<>();
    documentReferenceList.add(documentReference);
    IdentificationReport identificationReport = IdentificationReport.builder()
                                                                    .reportId(UUID.randomUUID().toString())
                                                                    .serverIdentity("https://test.governikus-eid.de/gov_autent/async")
                                                                    .reportTime(Instant.now())
                                                                    .identificationTime(Instant.now())
                                                                    .levelOfAssurance(LevelOfAssurance.EIDAS_LOW)
                                                                    .documentReferences(documentReferenceList)
                                                                    .build();
    JsonObject resource = identificationReport.toJson();

    OutputUnit result = SchemaValidator.validateJsonObject(SchemaConstants.Locations.IDENTIFICATION_REPORT_SCHEMA_LOCATION,
                                                           resource);

    Assertions.assertFalse(result.getValid(), result.getError());
    Assertions.assertTrue(result.getErrors()
                                .stream()
                                .anyMatch(error -> error.getInstanceLocation().endsWith("documentReferences")));
    Assertions.assertTrue(result.getErrors()
                                .stream()
                                .anyMatch(error -> error.getInstanceLocation().endsWith("customField")));
  }

  /**
   * makes sure that the level of assurance values are correctly deserialized
   */
  @ParameterizedTest
  @CsvSource({"http://eidas.europa.eu/LoA/high,EIDAS_HIGH", "http://eidas.europa.eu/LoA/substantial,EIDAS_SUBSTANTIAL",
              "http://eidas.europa.eu/LoA/low,EIDAS_LOW",
              "http://eidas.europa.eu/NotNotified/LoA/high,EIDAS_NOT_NOTIFIED_HIGH",
              "http://eidas.europa.eu/NotNotified/LoA/substantial,EIDAS_NOT_NOTIFIED_SUBSTANTIAL",
              "http://eidas.europa.eu/NotNotified/LoA/low,EIDAS_NOT_NOTIFIED_LOW",
              "http://bsi.bund.de/eID/LoA/hoch,BSI_EID_HIGH",
              "http://bsi.bund.de/eID/LoA/substantiell,BSI_EID_SUBSTANTIAL",
              "http://bsi.bund.de/eID/LoA/normal,BSI_EID_LOW", "unknown,UNKNOWN", ",UNKNOWN"})
  public void testLevelOfAssuranceIsDeserializable(String levelOfAssuranceValue, LevelOfAssurance expectedEnumValue)
  {
    JsonObject jsonObject = JsonObject.of("levelOfAssurance", levelOfAssuranceValue);
    IdentificationReport identificationReport = Assertions.assertDoesNotThrow(() -> {
      return IdentificationReport.fromJson(jsonObject.encode());
    });
    Assertions.assertEquals(expectedEnumValue, identificationReport.getLevelOfAssurance());
  }

  /**
   * makes sure that the subjectRef is correctly translated into the given type if set into the parser method
   */
  @ParameterizedTest
  // @formatter:off
  @CsvSource({IDENTIFICATION_REPORT_2_0
                + ",https://raw.githubusercontent.com/Governikus/IdentificationReport/2.0.0/schema/eid-card.json"
                + ",de.governikus.identification.report.objects.subjects.EidCardPersonRef",
              IDENTIFICATION_REPORT_WITH_EID_CARD_SUBJECT_2_0
                + ",https://raw.githubusercontent.com/Governikus/IdentificationReport/2.0.0/schema/eid-card.json"
                + ",de.governikus.identification.report.objects.subjects.EidCardPersonRef"})
  // @formatter:on
  public <T extends SubjectRef> void testSubjectRefIsCorrectlyParsedBySubjectRefType(String jsonLocation,
                                                                                     String subjectRefType,
                                                                                     Class<T> expectecClassType)
  {
    final String json = readResourceFile(jsonLocation);
    JsonObject jsonObject = new JsonObject(json);
    jsonObject.put("subjectRefType", subjectRefType);

    IdentificationReport<T> identificationReport = Assertions.assertDoesNotThrow(() -> {
      return (IdentificationReport<T>)IdentificationReport.fromJson(jsonObject.toString());
    });
    MatcherAssert.assertThat(identificationReport.getSubjectRef().getClass(),
                             Matchers.typeCompatibleWith(expectecClassType));
  }

  /**
   * makes sure that the subjectRef is correctly translated into the correct type if the subjectRefType is used
   * to identify the objects subtype
   */
  @ParameterizedTest
  // @formatter:off
  @CsvSource({IDENTIFICATION_REPORT_2_0 + ",de.governikus.identification.report.objects.subjects.EidCardPersonRef",
              IDENTIFICATION_REPORT_WITH_EID_CARD_SUBJECT_2_0
                + ",de.governikus.identification.report.objects.subjects.EidCardPersonRef",
              FINK_USER_ACCOUNT_MINIMAL_REPORT
                + ",de.governikus.identification.report.objects.subjects.FinkPersonRefMinimal"})
  // @formatter:on
  public <T extends SubjectRef> void testSubjectRefIsCorrectlyParsed(String jsonLocation, Class<T> subjectRefType)
  {
    final String json = readResourceFile(jsonLocation);
    IdentificationReport<T> identificationReport = Assertions.assertDoesNotThrow(() -> {
      return IdentificationReport.fromJson(json, subjectRefType);
    });
    MatcherAssert.assertThat(identificationReport.getSubjectRef().getClass(),
                             Matchers.typeCompatibleWith(subjectRefType));
    Assertions.assertTrue(identificationReport.validate());
  }

  /**
   * verifies that the values of {@link EidCardPersonRef} are set
   */
  @Test
  public <T extends SubjectRef> void testValuesOfEidAuthAreSet()
  {
    final String json = readResourceFile(IDENTIFICATION_REPORT_2_0);
    IdentificationReport<T> identificationReport = Assertions.assertDoesNotThrow(() -> {
      return (IdentificationReport<T>)IdentificationReport.fromJson(json, EidCardPersonRef.class);
    });

    Assertions.assertEquals("be4f9806-0b5f-45c3-a008-96fd2750f8cb", identificationReport.getReportId());
    Assertions.assertEquals("https://test.governikus-eid.de/gov_autent/async",
                            identificationReport.getServerIdentity());
    Assertions.assertEquals(Instant.parse("2020-06-25T10:20:39Z"), identificationReport.getReportTime());
    Assertions.assertEquals(Instant.parse("2020-06-25T10:19:54Z"), identificationReport.getIdentificationTime());
    Assertions.assertEquals("successful identification sent by SAML-Assertion", identificationReport.getIdStatement());
    Assertions.assertEquals(LevelOfAssurance.EIDAS_HIGH, identificationReport.getLevelOfAssurance());

    EidCardPersonRef eidCardPersonRef = (EidCardPersonRef)identificationReport.getSubjectRef();
    Assertions.assertEquals("John", eidCardPersonRef.getGivenName());
    Assertions.assertEquals("Doe", eidCardPersonRef.getFamilyName());
  }

}
