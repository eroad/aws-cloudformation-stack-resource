package nz.co.eroad.concourse.resource.cloudformation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class VersionInput {
  private final Version version;
  private final Source source;

  @JsonCreator
  public VersionInput(@JsonProperty("version") Version version,
      @JsonProperty("source") Source source) {
    this.version = version;
    this.source = source;
  }

  public Version getVersion() {
    return version;
  }

  public Source getSource() {
    return source;
  }
}
