package de.governikus.identification.report.setup;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

import lombok.SneakyThrows;


/**
 * author Pascal Knueppel <br>
 * created at: 04.10.2019 - 20:07 <br>
 * <br>
 */
public interface FileReferences
{

  String BASE_PATH = "/de/governikus/identification";

  String IDENTIFICATION_REPORT_2_0 = BASE_PATH + "/report/identification-report-2.0.json";

  String IDENTIFICATION_REPORT_WITH_EID_CARD_SUBJECT_2_0 = BASE_PATH
                                                           + "/report/identification-report-with-natural-person-subject-2.0.json";

  String FINK_USER_ACCOUNT_MINIMAL_REPORT = BASE_PATH + "/report/fink-user-account-minimal-report.json";

  String UNIT_TEST_KEYSTORE = BASE_PATH + "/keys/unit-test.jks";

  /**
   * reads a file from the test-resources and modifies the content
   *
   * @param resourcePath the path to the resource
   * @return the resource read into a string value
   */
  default String readResourceFile(String resourcePath)
  {
    return readResourceFile(resourcePath, null);
  }

  /**
   * reads a file from the test-resources and modifies the content
   *
   * @param resourcePath the path to the resource
   * @param changeResourceFileContent a function on the file content to modify the return string
   * @return the resource read into a string value
   */
  default String readResourceFile(String resourcePath, Function<String, String> changeResourceFileContent)
  {
    try (InputStream resourceInputStream = getClass().getResourceAsStream(resourcePath))
    {
      if (resourceInputStream == null)
      {
        throw new IllegalArgumentException("Resource not found in given location: " + resourcePath);
      }
      Scanner s = new Scanner(resourceInputStream).useDelimiter("\\A");

      String content = s.hasNext() ? s.next() : "";
      if (changeResourceFileContent != null)
      {
        content = changeResourceFileContent.apply(content);
      }
      return content;
    }
    catch (IOException e)
    {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  /**
   * reads the keystore from the test-resources and returns it
   */
  @SneakyThrows
  default KeyStore getUnitTestKeystore()
  {
    try (InputStream inputStream = getClass().getResourceAsStream(UNIT_TEST_KEYSTORE))
    {
      KeyStore keyStore = KeyStore.getInstance("JKS");
      keyStore.load(inputStream, "123456".toCharArray());
      return keyStore;
    }
  }

  /**
   * @return the aliases and passwords of the entries within the keystores.
   */
  default List<KeystoreEntry> getUnitTestKeystoreEntries()
  {
    return Arrays.asList(// rsa keys
                         new KeystoreEntry("unit-test-rsa", "unit-test", "RSA", 2048),
                         new KeystoreEntry("goldfish-rsa", "123456", "RSA", 2048),
                         new KeystoreEntry("localhost-rsa", "123456", "RSA", 2048),
                         // ec keys
                         new KeystoreEntry("goldfish-ec", "123456", "EC", 256),
                         new KeystoreEntry("localhost-ec", "123456", "EC", 384),
                         new KeystoreEntry("unit-test-ec", "unit-test", "EC", 521),
                         // certificate entries without private keys
                         new KeystoreEntry("cert-ec", null, "EC", 256),
                         new KeystoreEntry("cert-rsa", null, "RSA", 2048));
  }
}
