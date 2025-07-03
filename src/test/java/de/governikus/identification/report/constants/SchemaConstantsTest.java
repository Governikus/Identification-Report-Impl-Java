package de.governikus.identification.report.constants;

import java.io.InputStream;
import java.util.Scanner;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.vertx.core.json.JsonObject;
import lombok.SneakyThrows;


/**
 * @author Pascal Knüppel
 * @since 06.11.2022
 */
public class SchemaConstantsTest
{

  /**
   * verifies that the ids within the schema do match their corresponding constants within the
   * {@link SchemaConstants} class
   *
   * @param fileLocation the location of the file that should contain the expected id
   * @param schemaId the id that must match the id within the given file
   */
  @SneakyThrows
  @ParameterizedTest
  @CsvSource({SchemaConstants.Locations.IDENTIFICATION_REPORT_SCHEMA_LOCATION + ","
              + SchemaConstants.Ids.IDENTIFICATION_REPORT_2_0_ID,
              SchemaConstants.Locations.FINK_USER_ACCOUNT_MINIMAL_SCHEMA_LOCATION + ","
                                                                  + SchemaConstants.Ids.FINK_PERSON_REF_MINIMAL_ID,
              SchemaConstants.Locations.EID_CARD_SCHEMA_LOCATION + "," + SchemaConstants.Ids.EID_CARD_PERSON_REF_ID})
  public void testAuthenticationSchemaConstantsMatchFileIds(String fileLocation, String schemaId)
  {
    final String fileInput;
    try (InputStream inputStream = getClass().getResourceAsStream(fileLocation))
    {
      Scanner s = new Scanner(inputStream).useDelimiter("\\A");
      fileInput = s.hasNext() ? s.next() : "";
    }
    JsonObject schemaJson = new JsonObject(fileInput);
    Assertions.assertEquals(schemaJson.getString("$id"), schemaId);
  }

}
