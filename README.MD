# Identification Report Impl Java

## Description

This project provides a simple Java support API to easily create and validate Identification Reports as described in
https://github.com/Governikus/IdentificationReport.

---

## Supported Versions

This implementation supports the version `2.0.0` of the `Identification Report`

### Supported `subjectRef`-types

| Version | Authentication Object Schema ID                                                                                 | `SubjectRef`-subtype        |
|:--------|:----------------------------------------------------------------------------------------------------------------|:----------------------------|
| 2.0.0   | https://raw.githubusercontent.com/Governikus/IdReport-SubjectRefSchemas/2.0.0/fink/person-ref-minimal-fink.json | FinkPersonRefMinimal.class  |
| 2.0.0   | https://raw.githubusercontent.com/Governikus/IdReport-SubjectRefSchemas/2.0.0/eid/person-ref-eid-card.json      | EidCardPersonRef.class      |

### Extendable

The supported `subjectRef`-types can be manually extended without changing the API. See below in the section
[How to use](#how-to-use).

---

**This project requires JDK 8 or higher**

Please note that some Elliptic Curve algorithms will require at least JDK 11.

```xml

<repositories>
    <repository>
        <id>nexus-ext-governikus</id>
        <url>https://nexus-ext.governikus.de/nexus/content/groups/public/</url>
    </repository>
</repositories>

<dependency>
<groupId>de.governikus</groupId>
<artifactId>identification-report-impl-java</artifactId>
<version>${project.version}</version>
</dependency>
```

---
**Note:**

If the project does not compile within your IDE install the "lombok" plugin for your IDE and restart it.

---

## Supported Features

- [x] JSON Schema validation of the Identification Report
- [x] Conversion of Identification Reports into and from JWT
  - [x] JWS
  - [x] Supported Key Types: `RSA` and `EC`
- [x] easy conversion from and into strings

---

## Basics

The identification report is a composition of two objects. The Identification Report itself and a subject that was
identified. The identified subject is a free JSON-object and is placed in the `subjectRef`-attribute.

The `subjectRef`-attribute is identified by the attribute `subjectRefType` that contains the schema id of the
referenced subject type.

```json
{
    "reportId": "be4f9806-0b5f-45c3-a008-96fd2750f8cb",
    "serverIdentity": "https://test.governikus-eid.de/gov_autent/async",
    "reportTime": "2020-06-25T10:20:39Z",
    "identificationTime": "2020-06-25T10:19:54Z",
    "subjectRefType": "${some-uri-to-an-expected-schema-describing-the-subject-ref}",
    "subjectRef": {
        "restrictedId": "1",
        "givenName": "John",
        "familyName": "Doe",
        "dateOfBirth": "1-1-1986",
        "placeOfBirth": "Berlin",
        "birthName": "Dorian",
        "placeOfResidence": {
            "street": "GROẞENHAINER STR. 133/135",
            "city": "DRESDEN",
            "state": "Dresden",
            "country": "D",
            "zipCode": "01129"
        }
    },
    "idStatement": "successful identification sent by SAML-Assertion",
    "levelOfAssurance": "http://eidas.europa.eu/LoA/high"
}
```

---

## SubjectRef Objects

This API provides an abstract object type with the name of `SubjectRef`. This object represents the Java
POJOs that can be placed within an `IdentificationReport`-object.

```java
public class IdentificationReport
{
  ...
  /**
   * The identified subject
   */
  private SubjectRef subjectRef;
  ...
}
```

---

## Object serialization and deserialization

Serialization and deserialization is done by the `jackson-databind` API.

---

## Pre-registered schemas

The schemas listed in the [Supported Versions](#supported-versions) section are pre-registered and must not be added
manually.

---

## How to use:

### Authentication Object Registering

This API allows automatic parsing of subtypes of the `SubjectRef`. In order to do so you should register
the objects schema-id with its corresponding subtype.

```java
final String mySchemaId = "some-schema-id-uri";
final Class<? extends SubjectRef> mySubType = EidCardAuthentication.class;
Schemas.addSchemaSubTypeReference(mySchemaId, mySubType);
```

### Create and validate

```java
IdentificationReport<EidCardPersonRef> identificationReport = 
                          IdentificationReport.<EidCardPersonRef>builder()
                                              .reportId(UUID.randomUUID().toString())
                                              .serverIdentity("https://some-idp-url.de")
                                              .reportTime(Instant.now())
                                              .identificationTime(Instant.now())
                                              .levelOfAssurance(LevelOfAssurance.EIDAS_LOW)
                                              .documentReferences(documentReferenceList)
                                              .build();
boolean isValid = identificationReport.validate();
OutputUnit outputUnit = identificationReport.getValidationResult();
```

### Parse from String

```java
final String json = "{the identification-report as json}";
final Class<?extends SubjectRef> subjectRefType = MySubjectRefType.class;
IdentificationReport identificationReport = IdentificationReport.fromJson(json, subjectRefType);
```

the type can be omitted if the `subjectRefType` parameter is present within the json document.

```java
final String json = "{the identification-report as json with subjectRefType}";
IdentificationReport identificationReport = IdentificationReport.fromJson(json);
```

### To JWS

```java
public static String toJws(PrivateKey privateKey, IdentificationReport identificationReport)
{
  final String json = identificationReport.toString();
  JwtHandler jwtHandler = new JwtHandler(privateKey, null);
  return jwtHandler.createJws(json);
}
```

### Parse from JWS

the `JwtHandler` resolves the algorithms automatically by analyzing the JWT-Header

```java
public static <T extends SubjectRef> IdentificationReport<T> fromJws(X509Certificate certificate,
                                                                     String json,
                                                                     Class<T> subjectRefType)
{
  JwtHandler jwtHandler = new JwtHandler(null, certificate);
  JwtHandler.PlainJwtData plainJwtData = jwtHandler.handleJwt(json);
  return IdentificationReport.fromJson(plainJwtData.getBody().toString(), subjectRefType);
}
```
