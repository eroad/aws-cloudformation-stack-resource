package nz.co.eroad.concourse.resource.cloudformation.check;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import nz.co.eroad.concourse.resource.cloudformation.aws.AwsClientFactory;
import nz.co.eroad.concourse.resource.cloudformation.pojo.Source;
import nz.co.eroad.concourse.resource.cloudformation.pojo.Version;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.Stack;

public class Check {

  private final CloudFormationClient cloudFormationClient;

  public Check(Source source) {
    this.cloudFormationClient = AwsClientFactory
        .cloudFormationClient(source.getRegion(), source.getCredentials());
  }

  public List<Version> run(String name, Version existing) {
    Optional<Stack> found = getStack(name);
    if (found.isEmpty()) {
      return Collections.emptyList();
    } else {
      Stack stack = found.get();
      switch (stack.stackStatus()) {
        case CREATE_COMPLETE:
        case UPDATE_COMPLETE:
        case ROLLBACK_COMPLETE:
        case IMPORT_COMPLETE:
          Version newVersion = Version.fromStack(stack);
          return Collections.singletonList(newVersion);
        case DELETE_COMPLETE:
        case CREATE_FAILED:
          return Collections.emptyList();
        default:
          if (existing == null) {
            return Collections.emptyList();
          } else {
            return Collections.singletonList(existing);
          }
      }
    }
  }

  private boolean stackNotFoundError(CloudFormationException cloudFormationException, String stack) {
    AwsErrorDetails awsErrorDetails = cloudFormationException.awsErrorDetails();
    return awsErrorDetails != null
        && "ValidationError".equals(awsErrorDetails.errorCode())
        && String.format("Stack with id %s does not exist", stack).equals(awsErrorDetails.errorMessage());
  }


  private Optional<Stack> getStack(String stackArnOrName) {
    DescribeStacksResponse describeStacksResponse;
    try {
      describeStacksResponse = cloudFormationClient.describeStacks(
          builder -> builder.stackName(stackArnOrName)
      );
    } catch (CloudFormationException e) {
      if (stackNotFoundError(e, stackArnOrName)) {
        return Optional.empty();
      }
      throw e;
    }
    if (!describeStacksResponse.hasStacks() || describeStacksResponse.stacks().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(describeStacksResponse.stacks().get(0));
  }
}
