package nz.co.eroad.concourse.resource.cloudformation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OutInput {
  private final Source source;
  private final Params params;

  @JsonCreator
  public OutInput(
      @JsonProperty(value = "source", required = true) Source source,
      @JsonProperty(value = "params", required = true) Params params
  ) {
    this.source = source;
    this.params = params;
  }

  public Source getSource() {
    return source;
  }

  public Params getParams() {
    return params;
  }
}
