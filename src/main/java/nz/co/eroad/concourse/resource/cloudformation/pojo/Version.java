package nz.co.eroad.concourse.resource.cloudformation.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Objects;
import software.amazon.awssdk.services.cloudformation.model.Stack;

public class Version {

  
  private final String arn;

  private final Instant time;

  public Version(String arn,
      Instant time) {
    this.arn = arn;
    this.time = time;
  }

  @JsonCreator
  public Version(@JsonProperty(value = "arn", required = true) String arn,
      @JsonProperty(value = "time", required = true) String time) {
    this.arn = arn;
    this.time = Instant.parse(time);
  }


  @JsonProperty("arn")
  public String getArn() {
    return arn;
  }

  @JsonProperty("time")
  @JsonFormat(shape = Shape.STRING)
  public Instant getTime() {
    return time;
  }
  
  public static Version fromStack(Stack stack) {
    Instant updateTime =
        stack.lastUpdatedTime() != null ? stack.lastUpdatedTime() : stack.creationTime();
    return new Version(stack.stackId(), updateTime);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Version version = (Version) o;
    return Objects.equals(arn, version.arn) &&
        Objects.equals(time, version.time);
  }

  @Override
  public int hashCode() {
    return Objects.hash(arn, time);
  }
}
