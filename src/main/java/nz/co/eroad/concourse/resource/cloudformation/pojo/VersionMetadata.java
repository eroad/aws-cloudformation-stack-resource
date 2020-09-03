package nz.co.eroad.concourse.resource.cloudformation.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;


public class VersionMetadata {
  private final Version version;
  private final List<Metadata> metadata;

  @JsonCreator
  public VersionMetadata(@JsonProperty("version") Version version,
      @JsonProperty("metadata") List<Metadata> metadata) {
    this.version = version;
    this.metadata = metadata;
  }


  @JsonProperty("version")
  public Version getVersion() {
    return version;
  }

  @JsonProperty("metadata")
  public List<Metadata> getMetadata() {
    return metadata;
  }
}
