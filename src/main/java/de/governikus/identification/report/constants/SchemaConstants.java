package de.governikus.identification.report.constants;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import de.governikus.identification.report.objects.subjects.EidCardPersonRef;
import de.governikus.identification.report.objects.subjects.FinkPersonRefMinimal;
import de.governikus.identification.report.objects.subjects.SubjectRef;
import de.governikus.identification.report.validation.SchemaValidator;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;


/**
 * @author Pascal Knüppel
 * @since 06.11.2022
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SchemaConstants
{

  /**
   * this map will remember each schema in order to prevent the schemas from being parsed over and over again
   */
  private static final Map<String, JsonSchema> SCHEMA_MAP = new HashMap<>();

  /**
   * contains references to subtype-classes of {@link SubjectRef}. These will be used to identify a fitting
   * subtype-class based on a schema identifier ($id-attribute). The natural-person-schema.json for example is
   * identified by the schema-id
   * "https://raw.githubusercontent.com/Governikus/IdentificationReport/2.0.0/schema/eid-authentication.json",
   * wich points to the subtype-class {@link EidCardPersonRef}.
   */
  private static final Map<String, Class<? extends SubjectRef>> AUTH_OBJECT_SUB_TYPE_REFERENCES = new HashMap<>();

  /*
   * registers the schema-ids with their corresponding subject-ref class types
   */
  static
  {
    AUTH_OBJECT_SUB_TYPE_REFERENCES.put("https://raw.githubusercontent.com/Governikus/IdentificationReport/2.0.0"
                                        + "/schema/eid-card.json",
                                        EidCardPersonRef.class);
    AUTH_OBJECT_SUB_TYPE_REFERENCES.put("https://raw.githubusercontent.com/Governikus/IdentificationReport/2.0.0"
                                        + "/schema/fink-user-account-minimal.json",
                                        FinkPersonRefMinimal.class);
  }

  /**
   * adds new custom references to the {@link #AUTH_OBJECT_SUB_TYPE_REFERENCES} map to be able to automatically
   * resolve the subtypes of an identification-report if the schemas id is present within the
   * subjectRefType-attribute
   *
   * @param schemaId the id of the schema that is referenced
   * @param authType the subytpe of {@link SubjectRef}
   */
  public static void addSchemaSubTypeReference(String schemaId, Class<? extends SubjectRef> authType)
  {
    AUTH_OBJECT_SUB_TYPE_REFERENCES.put(schemaId, authType);
  }

  /**
   * @param schemaId the schema identifier that identifies the java pojo subtype-class.
   * @return the java pojo subtype-class of {@link SubjectRef} belonging to the given schemaId
   */
  public static Class<? extends SubjectRef> getSubType(String schemaId)
  {
    return AUTH_OBJECT_SUB_TYPE_REFERENCES.get(schemaId);
  }

  /**
   * reads the schema from the given location and stores it within a static map in order to prevent continuous
   * parsing of the file
   *
   * @param schemaLocation the location of the schema file that should be retrieved as json schema instance
   */
  @SneakyThrows
  public static JsonSchema getSchema(String schemaLocation)
  {
    final String jsonSchemaString;
    JsonSchema schemaOptional = SCHEMA_MAP.get(schemaLocation);
    if (schemaOptional != null)
    {
      return schemaOptional;
    }

    try (InputStream inputStream = SchemaValidator.class.getResourceAsStream(schemaLocation))
    {
      jsonSchemaString = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }
    JsonObject object = new JsonObject(jsonSchemaString);
    JsonSchema schema = JsonSchema.of(object);

    SCHEMA_MAP.put(schemaLocation, schema);
    return schema;
  }

  /**
   * the schema id references
   */
  public static class Ids
  {

    public static final String IDENTIFICATION_REPORT_2_0_ID = "https://raw.githubusercontent.com/Governikus/Identification"
                                                              + "Report/2.0.0/schema/identification-report.json";

    public static final String FINK_PERSON_REF_MINIMAL_ID = "https://raw.githubusercontent.com/Governikus/IdReport-"
                                                            + "SubjectRefSchemas/2.0.0/fink/person-ref-minimal-fink.json";

    public static final String EID_CARD_PERSON_REF_ID = "https://raw.githubusercontent.com/Governikus/IdReport-"
                                                        + "SubjectRefSchemas/2.0.0/eid/person-ref-eid-card.json";
  }

  /**
   * the classpath locations of the schemas
   */
  public static class Locations
  {

    public static final String BASE_PATH = "/de/governikus/identification/report/schemas";

    public static final String IDENTIFICATION_REPORT_SCHEMA_LOCATION = BASE_PATH + "/identification-report-schema.json";

    public static final String EID_CARD_SCHEMA_LOCATION = BASE_PATH + "/person-ref-eid-card.json";

    public static final String FINK_USER_ACCOUNT_MINIMAL_SCHEMA_LOCATION = BASE_PATH + "/person-ref-minimal-fink.json";
  }
}
