package nz.co.eroad.concourse.resource.cloudformation.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Tag {
  private final String key;
  private final String value;

  @JsonCreator
  public Tag(@JsonProperty("Key") String key, @JsonProperty("Value") String value) {
    this.key = key;
    this.value = value;
  }

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }
}
