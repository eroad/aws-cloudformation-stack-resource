package nz.co.eroad.concourse.resource.cloudformation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Metadata {
  private final String name;
  private final String value;

  @JsonCreator
  public Metadata(@JsonProperty("name") String name, @JsonProperty("value") String value) {
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

}
