package de.governikus.identification.report.jwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECParameterSpec;
import java.util.Optional;

import com.nimbusds.jose.Header;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSSignerFactory;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.SignedJWT;

import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;


/**
 * @author Pascal Knueppel
 * @since 04.11.2022
 */
public class JwtHandler
{

  /**
   * key for signing or decrypting a jwt.
   */
  private final PrivateKey privateKey;

  /**
   * key for encrypting or validating a signature of a jwt
   */
  private final X509Certificate certificate;

  /**
   * Do not mix keys together that do not belong together or unexpected results will occur
   */
  @Builder
  public JwtHandler(PrivateKey privateKey, X509Certificate certificate)
  {
    this.privateKey = privateKey;
    this.certificate = certificate;
  }

  /**
   * creates a signed JWT (JWS) based on the given key material
   *
   * @param body the body that should either be encrypted or signed
   * @return the signed or encrypted JWT
   */
  @SneakyThrows
  public String createJws(String body)
  {
    JWSAlgorithm signatureAlgorithm = selectSignatureAlgorithm();
    JWSHeader jwsHeader = buildJwsHeader(signatureAlgorithm);
    return createSignedJwt(jwsHeader, body);
  }

  /**
   * verifies the signature of a signed JWT (JWS) and returns its plain data
   *
   * @param jwt the signed or encrypted JWT
   * @return the payload of the verified or decrypted content
   */
  @SneakyThrows
  public PlainJwtData handleJwt(String jwt)
  {
    final int numberOfJwtParts = jwt.split("\\.").length;
    if (numberOfJwtParts == 3)
    {
      return verifySignature(jwt);
    }
    else
    {
      throw new IllegalStateException("Unsupported JWT. Only compact JWS structures are supported for "
                                      + "signature verification.");
    }
  }

  /**
   * Create a jws header with the given algorithm and a certificate sha-256 thumbprint.
   */
  @SneakyThrows
  private JWSHeader buildJwsHeader(JWSAlgorithm signatureAlgorithm)
  {
    Base64URL sha256Thumbprint = getSha256Thumbprint();
    return new JWSHeader.Builder(signatureAlgorithm).x509CertSHA256Thumbprint(sha256Thumbprint).build();
  }

  /**
   * automatically selects the EC algorithm based on the given key type
   */
  private JWSAlgorithm selectSignatureAlgorithm()
  {
    switch (certificate.getPublicKey().getAlgorithm())
    {
      case "RSA":
        return JWSAlgorithm.RS512;
      case "EC":
        Integer keyLength = getKeyLength();
        switch (keyLength)
        {
          case 256:
            if (((ECPublicKey)certificate.getPublicKey()).getParams().toString().startsWith("secp256k1"))
            {
              return JWSAlgorithm.ES256K;
            }
            return JWSAlgorithm.ES256;
          case 384:
            return JWSAlgorithm.ES384;
          case 521:
            return JWSAlgorithm.ES512;
          default:
            throw new IllegalArgumentException(String.format("Unsupported key length for EC key type: %s-bit",
                                                             keyLength));
        }
      default:
        throw new IllegalArgumentException(String.format("Unsupported key type '%s'",
                                                         certificate.getPublicKey().getAlgorithm()));
    }
  }

  /**
   * adds the sha-256 thumbprint to the JWT header
   */
  @SneakyThrows
  private Base64URL getSha256Thumbprint()
  {
    return Base64URL.encode(MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded()));
  }

  /**
   * verifies the signature based on the data within the header
   */
  @SneakyThrows
  private PlainJwtData verifySignature(String jws)
  {
    SignedJWT signedJwt = SignedJWT.parse(jws);
    JWSVerifier jwsVerifier = getVerifier(signedJwt.getHeader());
    boolean isValid = signedJwt.verify(jwsVerifier);
    if (!isValid)
    {
      throw new IllegalStateException("Signature validation has failed with signature key");
    }
    return PlainJwtData.builder()
                       .operationExecuted(OperationExecuted.SIGNATURE_VERIFIED)
                       .header(signedJwt.getHeader())
                       .body(signedJwt.getPayload())
                       .build();
  }

  /**
   * builds a signature with the given key and builds the signed JWT from it
   *
   * @param jwsHeader contains the algorithm to use for signature
   * @param body the body to sign together with the header
   * @return the signed JWT
   */
  @SneakyThrows
  private String createSignedJwt(JWSHeader jwsHeader, String body)
  {

    JWK jwk = toJwk();
    JWSSigner jwsSigner = new DefaultJWSSignerFactory().createJWSSigner(jwk, jwsHeader.getAlgorithm());
    Payload payload = new Payload(body);
    String headerAndBody = jwsHeader.toBase64URL().toString() + "." + payload.toBase64URL().toString();
    Base64URL signature = jwsSigner.sign(jwsHeader, headerAndBody.getBytes(StandardCharsets.UTF_8));
    SignedJWT jws = new SignedJWT(jwsHeader.toBase64URL(), Base64URL.encode(body), signature);
    return jws.serialize();
  }

  /**
   * parsed an RSA or EC key to its JWK representation
   */
  private JWK toJwk()
  {
    switch (certificate.getPublicKey().getAlgorithm())
    {
      case "RSA":
        return toRsaJwk();
      case "EC":
        return toEcJwk();
      default:
        return null; // should not happen due to previous validation
    }
  }

  /**
   * parses an EC key pair into its EC JWK representation
   */
  private ECKey toEcJwk()
  {
    ECPublicKey ecPublicKey = (ECPublicKey)certificate.getPublicKey();
    ECParameterSpec ecParameterSpec = ecPublicKey.getParams();
    Curve curve = Curve.forECParameterSpec(ecParameterSpec);
    ECKey.Builder builder = new ECKey.Builder(curve, ecPublicKey);
    Optional.ofNullable(privateKey).ifPresent(builder::privateKey);
    return builder.build();
  }

  /**
   * parses an RSA key pair into its RSA JWK representation
   */
  private RSAKey toRsaJwk()
  {
    RSAKey.Builder builder = new RSAKey.Builder((RSAPublicKey)certificate.getPublicKey());
    Optional.ofNullable(privateKey).ifPresent(builder::privateKey);
    return builder.build();
  }

  /**
   * retrieves the signature verifier based on the data within the JWS header
   */
  @SneakyThrows
  private JWSVerifier getVerifier(JWSHeader header)
  {
    boolean isRsaAlgorithm = JWSAlgorithm.Family.RSA.stream().anyMatch(rsa -> rsa.equals(header.getAlgorithm()));
    if (isRsaAlgorithm)
    {
      return new RSASSAVerifier((RSAPublicKey)certificate.getPublicKey());
    }

    boolean isEcAlgorithm = JWSAlgorithm.Family.EC.stream().anyMatch(ec -> ec.equals(header.getAlgorithm()));
    if (isEcAlgorithm)
    {
      return new ECDSAVerifier((ECPublicKey)certificate.getPublicKey());
    }

    String errorMessage = String.format("Unsupported algorithm found '%s'", header.getAlgorithm());
    throw new IllegalArgumentException(errorMessage);
  }

  /**
   * gets the key length of the currently provided key material
   *
   * @return the length of the key.
   */
  private Integer getKeyLength()
  {
    PublicKey publicKey = certificate.getPublicKey();
    switch (publicKey.getAlgorithm())
    {
      case "RSA":
        return ((RSAPublicKey)publicKey).getModulus().bitLength();
      case "EC":
        return ((ECPublicKey)publicKey).getParams().getOrder().bitLength();
      default:
        throw new IllegalStateException(String.format("Not supporting keys of type '%s'", publicKey.getAlgorithm()));
    }
  }

  /**
   * represents the plain data of a JWT
   */
  @Getter
  @Builder
  public static class PlainJwtData
  {

    /**
     * tells us which operation was executed on the JWT if its signature was verified or if it was decrypted
     */
    private final OperationExecuted operationExecuted;

    /**
     * the JWT header
     */
    private final Header header;

    /**
     * the JWT body
     */
    private final Payload body;
  }

  public static enum OperationExecuted
  {
    SIGNATURE_VERIFIED, DECRYPTED
  }
}
