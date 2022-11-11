package de.governikus.identification.report.objects.subjects;

import java.util.Map;

import de.governikus.identification.report.constants.SchemaConstants;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @author Pascal Knueppel
 * @since 02.11.2022
 */
@Data
@NoArgsConstructor
public class FinkPersonRefMinimal extends SubjectRef
{

  private String givenName;

  private String familyName;

  @Builder
  public FinkPersonRefMinimal(String givenName, String familyName, Map<String, Object> additionalProperties)
  {
    super(additionalProperties);
    this.givenName = givenName;
    this.familyName = familyName;
  }

  @Override
  protected String getSchemaLocation()
  {
    return SchemaConstants.Locations.FINK_USER_ACCOUNT_MINIMAL_SCHEMA_LOCATION;
  }
}
