package de.governikus.identification.report.jwt;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import de.governikus.identification.report.setup.FileReferences;
import de.governikus.identification.report.setup.KeystoreEntry;


/**
 * @author Pascal Knueppel
 * @since 07.11.2022
 */
public class JwtHandlerTest implements FileReferences
{

  /**
   * verifies that signing, validation of signatures, encryption and decryption works for different key-types of
   * RSA and EC.
   */
  @TestFactory
  public List<DynamicTest> testJwtHandlerTest()
  {
    List<DynamicTest> dynamicTestList = new ArrayList<>();

    final KeyStore keyStore = getUnitTestKeystore();

    final String content = readResourceFile(IDENTIFICATION_REPORT_2_0);

    for ( KeystoreEntry keystoreEntry : getUnitTestKeystoreEntries() )
    {
      PrivateKey privateKey = keystoreEntry.getPrivateKey(keyStore);
      X509Certificate certificate = keystoreEntry.getCertificate(keyStore);
      JwtHandler jwtHandler = new JwtHandler(privateKey, certificate);

      // add signature tests
      {
        if (privateKey != null)
        {
          final String testName = String.format("Sign and validate JWS with '%s' of type '%s' of length '%s'",
                                                keystoreEntry.getAlias(),
                                                keystoreEntry.getKeyAlgorithm(),
                                                keystoreEntry.getKeyLength());
          dynamicTestList.add(DynamicTest.dynamicTest(testName, () -> {
            String jws = jwtHandler.createJws(content);
            // make sure returned string is a compact JWS
            Assertions.assertEquals(3, jws.split("\\.").length);
            JwtHandler.PlainJwtData plainJwtData = jwtHandler.handleJwt(jws);
            Assertions.assertEquals(content, plainJwtData.getBody().toString());
          }));
        }
      }
    }

    return dynamicTestList;
  }
}
