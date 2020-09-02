package nz.co.eroad.concourse.resource.cloudformation.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;


public class Source {
  private final String name;
  private final Region region;
  private final AwsBasicCredentials credentials;
  private final List<String> notificationArns;

  @JsonCreator
  public Source(
      @JsonProperty(value = "name", required = true) String name,
      @JsonProperty(value = "region", required = true) String region,
      @JsonProperty("access_key") String awsAccessKey,
      @JsonProperty("secret_key") String awsSecretKey,
      @JsonProperty("notification_arns") List<String> notificationArns) {
    this.name = name;
    this.region = parseRegion(region);
    if (awsAccessKey != null && awsSecretKey != null) {
      this.credentials = AwsBasicCredentials.create(awsAccessKey, awsSecretKey);
    } else if ((awsAccessKey == null) != (awsSecretKey == null)) {
      throw new IllegalArgumentException("Both access_key and secret_key must be defined or not defined together!");
    } else {
      this.credentials = null;
    }
    this.notificationArns = notificationArns == null ? Collections.emptyList() : notificationArns;
  }

  public String getName() {
    return name;
  }

  public Region getRegion() {
    return region;
  }

  public AwsBasicCredentials getCredentials() {
    return credentials;
  }

  public List<String> getNotificationArns() {
    return notificationArns;
  }

  private static Region parseRegion(String region) {
    Optional<Region> any = Region.regions().stream()
        .filter(validRegion -> validRegion.id().equals(region)).findAny();
    if (any.isEmpty()) {
      throw new IllegalArgumentException("Could not find region with id " + region);
    }
    return any.get();
  }

}
