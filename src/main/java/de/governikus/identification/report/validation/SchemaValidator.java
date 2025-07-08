package de.governikus.identification.report.validation;

import java.util.Locale;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.ExecutionConfig;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;

import de.governikus.identification.report.constants.SchemaConstants;
import de.governikus.identification.report.utils.ObjectMapperUtil;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class SchemaValidator
{


  /**
   * Validates a POJO against a JSON schema from the given location.
   *
   * @param schemaLocation the schemas location that is used to validate the json object
   * @param pojo the object that should be validated
   * @return true if the object matches the schema definitions
   */
  public static boolean isJsonValid(String schemaLocation, Object pojo)
  {
    JsonNode node = ObjectMapperUtil.getObjectMapper().valueToTree(pojo);
    return isJsonValid(schemaLocation, node);
  }

  /**
   * Validates a JsonNode against a JSON schema from the given location.
   *
   * @param schemaLocation the schemas location that is used to validate the json object
   * @param jsonNode the json node to validate
   * @return true if the object matches the schema definitions
   */
  public static boolean isJsonValid(String schemaLocation, JsonNode jsonNode)
  {
    Set<ValidationMessage> errors = validateJsonObject(schemaLocation, jsonNode);
    errors.forEach(error -> log.info(error.getMessage()));
    return errors.isEmpty();
  }

  /**
   * Validates a POJO and returns the set of validation messages.
   *
   * @param schemaLocation the schemas location
   * @param pojo the object to validate
   * @return set of validation errors (empty if valid)
   */
  public static Set<ValidationMessage> validateJsonObject(String schemaLocation, Object pojo)
  {
    JsonNode node = ObjectMapperUtil.getObjectMapper().valueToTree(pojo);
    return validateJsonObject(schemaLocation, node);
  }

  /**
   * Validates a JsonNode and returns the set of validation messages.
   *
   * @param schemaLocation the schemas location
   * @param jsonNode the json node to validate
   * @return set of validation errors (empty if valid)
   */
  public static Set<ValidationMessage> validateJsonObject(String schemaLocation, JsonNode jsonNode)
  {
    JsonSchema schema = SchemaConstants.getSchema(schemaLocation);
    return schema.validate(jsonNode, executionContext -> {
      ExecutionConfig executionConfig = new ExecutionConfig();
      executionConfig.setLocale(Locale.US);
      executionContext.setExecutionConfig(executionConfig);
    });
  }

  /**
   * Uses the given JSON schema to validate the json node.
   *
   * @param schema used to validate the json node with the existing schema
   * @param jsonNode the json node that should be validated
   * @return set of validation errors (empty if valid)
   */
  public static Set<ValidationMessage> validateJsonObject(JsonSchema schema, JsonNode jsonNode)
  {
    return schema.validate(jsonNode);
  }
}
