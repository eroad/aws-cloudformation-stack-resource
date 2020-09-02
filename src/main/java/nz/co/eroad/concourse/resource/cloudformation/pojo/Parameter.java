package nz.co.eroad.concourse.resource.cloudformation.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Parameter {
  private final String parameterKey;

  private final String parameterValue;

  private final Boolean usePreviousValue;

  private final String resolvedValue;

  @JsonCreator
  public Parameter(@JsonProperty("ParameterKey") String parameterKey,
      @JsonProperty("ParameterValue") String parameterValue,
      @JsonProperty("UsePreviousValue")  Boolean usePreviousValue,
      @JsonProperty("ResolvedValue") String resolvedValue) {
    this.parameterKey = parameterKey;
    this.parameterValue = parameterValue;
    this.usePreviousValue = usePreviousValue;
    this.resolvedValue = resolvedValue;
  }

  public String getParameterKey() {
    return parameterKey;
  }

  public String getParameterValue() {
    return parameterValue;
  }

  public Boolean getUsePreviousValue() {
    return usePreviousValue;
  }

  public String getResolvedValue() {
    return resolvedValue;
  }
}
