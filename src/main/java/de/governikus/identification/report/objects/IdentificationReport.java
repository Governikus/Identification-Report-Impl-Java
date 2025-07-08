package de.governikus.identification.report.objects;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.governikus.identification.report.constants.SchemaConstants;
import de.governikus.identification.report.objects.subjects.SubjectRef;
import de.governikus.identification.report.utils.ObjectMapperUtil;
import de.governikus.identification.report.validation.SchemaValidator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 06.09.2022
 */
@Slf4j
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IdentificationReport<T extends SubjectRef>
{

  /**
   * Must be unique to a single report. GUID is recommended here
   */
  private String reportId;

  /**
   * This is a server id as software instance. This could be the server IP address, servername or the URL of the
   * SAML endpoint providing this identity report (like ServerIdentity in XTA2 Version 4)
   */
  private String serverIdentity;

  /**
   * Datetime of the creation of this report (like ReportTime in XTA2 Version 4)
   */
  private Instant reportTime;

  /**
   * Datetime of the identification process as stated in the original id statement
   */
  private Instant identificationTime;

  /**
   * Used to identify how the entity was authenticated to determine the level of assurance of this
   * authentication. See also attribute #/properties/levelOfAssurance
   */
  private String trustFramework;

  /**
   * in eIDAS contexts an id scheme (if it is notified) has a known LoA. When used in a national context only
   * the values from the authority bsi.bund.de SHALL be used, which correspond to the levels as defined in
   * [TR-03107-1]. See also BSI TR-03130
   */
  private LevelOfAssurance levelOfAssurance;

  /**
   * This is the value to indicate the status of the identification process. In case of failure the
   * corresponding reason shall be stated in the idStatement attribute
   */
  private IdStatus idStatus;

  /**
   * This is the error reason or some additional information in case of unknown or success situations. The
   * corresponding message should be human-readable
   */
  private String idStatement;

  /**
   * Describes the object type that is to be expected within the #/properties/subjectRef attribute in form of a
   * schema-id.
   */
  private String subjectRefType;

  /**
   * The identified subject
   */
  private T subjectRef;

  /**
   * The element corresponds to the TransactionContext in BSI TR-03130 which MAY be used to transmit context
   * information like an ID or a hash. To have a link between this identification report and the service for
   * which the identification process was started
   */
  private List<String> contextInformation;

  /**
   * This element can contain references to documents including their hashes. This is an optional attribut.
   */
  private List<ObjectNode> documentReferences;

  /**
   * additional elements that should be added to the json structure
   */
  @JsonIgnore // prevents the map from getting serialized in a nested property named "keyValueMap"
  @JsonAnyGetter // will add all properties within the map as flattened properties into the json structure
  @Builder.Default
  private Map<String, Object> keyValueMap = new HashMap<>();

  /**
   * will parse the given json string into an identification report with an internal {@link #subjectRef} that is
   * determined automatically by the inline value of {@link #subjectRefType}
   *
   * @param json the json string to parse evaluating the value of {@link #subjectRefType}
   * @return the representational identification report
   */
  @JsonIgnore
  @SneakyThrows
  public static IdentificationReport<? extends SubjectRef> fromJson(String json)
  {
    JsonNode node = ObjectMapperUtil.getObjectMapper().readTree(json);
    return fromJson(node);
  }

  /**
   * will parse the given json string into an identification report with an internal {@link #subjectRef} that is
   * determined automatically by the inline value of {@link #subjectRefType}
   *
   * @param jsonNode the json object to parse evaluating the value of {@link #subjectRefType}
   * @return the representational identification report
   */
  @JsonIgnore
  @SneakyThrows
  public static <R extends SubjectRef> IdentificationReport<R> fromJson(JsonNode jsonNode)
  {
    final String subjectRefKey = "subjectRef";
    final String subjectRefTypeKey = "subjectRefType";

    // 1. Determine the subjectRef type using the schema id
    String subjectRefType = jsonNode.has(subjectRefTypeKey) ? jsonNode.get(subjectRefTypeKey).asText() : null;
    Class<R> type = (Class<R>)SchemaConstants.getSubType(subjectRefType);

    JsonNode subjectRefJson = jsonNode.get(subjectRefKey);

    if (type == null && subjectRefJson != null)
    {
      throw new IllegalStateException(String.format("Unregistered schema with id '%s' found. Register the schema first with its corresponding subtype.",
                                                    subjectRefType));
    }

    // 2. Remove subjectRef from the main node before deserialization
    ObjectNode copy = jsonNode.deepCopy();
    copy.remove(subjectRefKey);

    IdentificationReport<R> identificationReport = ObjectMapperUtil.getObjectMapper()
                                                                   .treeToValue(copy, IdentificationReport.class);

    if (type == null)
    {
      return identificationReport;
    }

    R subjectRef = ObjectMapperUtil.getObjectMapper().treeToValue(subjectRefJson, type);
    identificationReport.setSubjectRef(subjectRef);
    return identificationReport;
  }

  /**
   * will parse the given json string into an identification report with an internal {@link #subjectRef} of the
   * given type
   *
   * @param json the json string to parse
   * @param subjectRefType the type of the {@link #subjectRef}. This should be possible to determine by
   *          evaluating the value of {@link #subjectRefType}
   * @return the representational identification report
   * @param <T> a subtype of {@link SubjectRef}
   */
  @JsonIgnore
  @SneakyThrows
  public static <T extends SubjectRef> IdentificationReport<T> fromJson(String json, Class<T> subjectRefType)
  {
    JsonNode node = ObjectMapperUtil.getObjectMapper().readTree(json);
    return fromJson(node, subjectRefType);
  }

  /**
   * will parse the given json string into an identification report with an internal {@link #subjectRef} of the
   * given type
   *
   * @param jsonNode the json object to parse
   * @param subjectRefType the type of the {@link #subjectRef}. This should be possible to determine by
   *          evaluating the value of {@link #subjectRefType}
   * @return the representational identification report
   * @param <T> a subtype of {@link SubjectRef}
   */
  @JsonIgnore
  @SneakyThrows
  public static <T extends SubjectRef> IdentificationReport<T> fromJson(JsonNode jsonNode, Class<T> subjectRefType)
  {
    final String subjectRefKey = "subjectRef";

    // Extract and deserialize the subjectRef
    JsonNode subjectRefNode = jsonNode.get(subjectRefKey);
    T subjectRef = ObjectMapperUtil.getObjectMapper().treeToValue(subjectRefNode, subjectRefType);

    // Remove subjectRef from the main node before deserialization
    ObjectNode copy = jsonNode.deepCopy();
    copy.remove(subjectRefKey);

    // Deserialize the rest of the IdentificationReport
    IdentificationReport<T> identificationReport = ObjectMapperUtil.getObjectMapper()
                                                                   .treeToValue(copy, IdentificationReport.class);

    identificationReport.setSubjectRef(subjectRef);
    return identificationReport;
  }

  /**
   * verifies if this object matches against its schemas definition
   *
   * @return true if the schema validation succeeds, false else
   */
  public boolean validate()
  {
    Optional.ofNullable(subjectRef).ifPresent(SubjectRef::validate);
    return SchemaValidator.isJsonValid(SchemaConstants.Locations.IDENTIFICATION_REPORT_SCHEMA_LOCATION,
                                       ObjectMapperUtil.getObjectMapper().valueToTree(this));
  }

  /**
   * will parse instance into its json string representation
   */
  @JsonIgnore
  public JsonNode toJson()
  {
    return ObjectMapperUtil.getObjectMapper().valueToTree(this);
  }

  /**
   * will add flat added properties as values to this object
   */
  @JsonAnySetter
  public void add(String key, Object value)
  {
    keyValueMap.put(key, value);
  }

  /**
   * used by jackson databind. With this method we map the null-value to {@link LevelOfAssurance#UNKNOWN}
   */
  @JsonSetter
  public void setLevelOfAssurance(LevelOfAssurance levelOfAssurance)
  {
    if (levelOfAssurance == null)
    {
      this.levelOfAssurance = LevelOfAssurance.UNKNOWN;
      return;
    }
    this.levelOfAssurance = levelOfAssurance;
  }

  @Override
  public String toString()
  {
    return toJson().toString();
  }

  /**
   * Get the report time truncated to milliseconds
   *
   * @return report time with milliseconds
   */
  public Instant getReportTime()
  {
    if (reportTime == null)
    {
      return null;
    }
    return reportTime.truncatedTo(ChronoUnit.MILLIS);
  }

  /**
   * Get the identification date truncated to milliseconds
   *
   * @return report time with milliseconds
   */
  public Instant getIdentificationTime()
  {
    if (identificationTime == null)
    {
      return null;
    }
    return identificationTime.truncatedTo(ChronoUnit.MILLIS);
  }
}
