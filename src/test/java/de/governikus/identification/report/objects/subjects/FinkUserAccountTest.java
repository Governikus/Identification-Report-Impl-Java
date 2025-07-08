package de.governikus.identification.report.objects.subjects;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * @author Pascal Knueppel
 * @since 02.11.2022
 */
public class FinkUserAccountTest
{

  /**
   * verifies that the document is validating successfully if a firstname and lastname is present
   */
  @Test
  public void testIsValidatedSuccessfully()
  {
    FinkPersonRefMinimal authentication = FinkPersonRefMinimal.builder()
                                                              .givenName("Max")
                                                              .familyName("Mustermann")
                                                              .build();
    Assertions.assertTrue(authentication.validate());
  }

  /**
   * verifies that the schema-id is correctly extracted from the json schema that represents the specific object
   */
  @Test
  public void testSchemaIdIsCorrectlyExtracted()
  {
    FinkPersonRefMinimal authentication = FinkPersonRefMinimal.builder()
                                                              .givenName("Max")
                                                              .familyName("Mustermann")
                                                              .build();
    Assertions.assertEquals("https://raw.githubusercontent.com/Governikus/IdReport-SubjectRefSchemas/"
                            + "2.0.0/fink/person-ref-minimal-fink.json",
                            authentication.getSchemaId());
  }
}
