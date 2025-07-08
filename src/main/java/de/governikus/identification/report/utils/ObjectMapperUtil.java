package de.governikus.identification.report.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.experimental.UtilityClass;


/**
 * Provide an instance of {@link ObjectMapper} that is already configured to correctly use java-8-time
 */
@UtilityClass
public class ObjectMapperUtil
{

  private static ObjectMapper objectMapper;

  /**
   * Get the object mapper
   */
  public static ObjectMapper getObjectMapper()
  {
    if (objectMapper == null)
    {
      objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());
      objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }
    return objectMapper;
  }
}
