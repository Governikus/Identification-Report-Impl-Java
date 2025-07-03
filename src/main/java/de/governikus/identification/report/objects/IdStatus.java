package de.governikus.identification.report.objects;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;


/**
 * describes the status of the identification report
 *
 * @author Pascal Knueppel
 * @since 10.11.2022
 */
@AllArgsConstructor
public enum IdStatus
{

  SUCCESS("success"), INCOMPLETE("incomplete"), FAILURE("failure"), UNKNOWN("unknown");

  private final String value;

  @JsonValue
  public String value()
  {
    return this.value;
  }

  @Override
  public String toString()
  {
    return this.value;
  }
}
