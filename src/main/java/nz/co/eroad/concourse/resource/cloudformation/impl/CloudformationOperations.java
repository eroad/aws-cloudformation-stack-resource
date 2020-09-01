package nz.co.eroad.concourse.resource.cloudformation.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import nz.co.eroad.concourse.resource.cloudformation.Source;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.ResourceStatus;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.StackEvent;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackRequest;

public class CloudformationOperations {

  private final static Set<ResourceStatus> UPDATABLE_STATUSES = new HashSet<>(
      Arrays.asList(ResourceStatus.CREATE_COMPLETE, ResourceStatus.UPDATE_COMPLETE, ResourceStatus.IMPORT_COMPLETE, ResourceStatus.IMPORT_ROLLBACK_COMPLETE, ResourceStatus.DELETE_COMPLETE)
  );

  private static final ClientOverrideConfiguration unlimitedRetry = ClientOverrideConfiguration.builder()
      .retryPolicy(RetryPolicy.builder(RetryMode.STANDARD).numRetries(Integer.MAX_VALUE).build())
      .build();

  private final CloudFormationClient cloudFormationClient;

  public CloudformationOperations(Region region, AwsBasicCredentials awsBasicCredentials) {
    this.cloudFormationClient = CloudFormationClient.builder()
        .overrideConfiguration(unlimitedRetry)
        .credentialsProvider(awsBasicCredentials == null ? null : StaticCredentialsProvider.create(awsBasicCredentials))
        .httpClientBuilder(UrlConnectionHttpClient.builder())
        .region(region)
        .build();
  }
  
  private boolean stackNotFoundError(CloudFormationException cloudFormationException, String stack) {
    AwsErrorDetails awsErrorDetails = cloudFormationException.awsErrorDetails();
    return awsErrorDetails != null
        && "ValidationError".equals(awsErrorDetails.errorCode())
        && String.format("Stack with id %s does not exist", stack).equals(awsErrorDetails.errorMessage());
  }


  public Optional<Stack> getStack(String stackArnOrName) {
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



  private Optional<StackEvent> awaitStackStable(String stackName)  {
    Optional<StackEvent> lastEvent = getLastEvent(stackName);
    if (lastEvent.isPresent() && !isTerminatingEvent(stackName, lastEvent.get())) {
      System.err.println("Another update is in progress. Please stand by.");

      do {
        try {
          Thread.sleep(10000);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(e);
        }
        lastEvent = getLastEvent(stackName);
      } while (lastEvent.isPresent() && !isTerminatingEvent(stackName, lastEvent.get()));
    }
    return lastEvent;

  }

  public Iterator<StackEvent> upsert(String requestToken, Source source, Parameters parameters) {
    Optional<StackEvent> lastUpdateEvent = awaitStackStable(source.getName());

    final String stackId;

    if (lastUpdateEvent.isPresent()) {
      StackEvent lastEvent = lastUpdateEvent.get();

      if (UPDATABLE_STATUSES.contains(lastEvent.resourceStatus())) {
        System.err.println("Stack exists, updating it.");

        UpdateStackRequest updateStackRequest = UpdateStackRequest.builder()
            .stackPolicyDuringUpdateBody(source.getName())
            .capabilities(parameters.getCapabilities())
            .parameters(parameters.getParameters())
            .tags(parameters.getTags())
            .templateBody(parameters.getTemplate())
            .notificationARNs(source.getNotificationArns())
            .clientRequestToken(requestToken)
            .build();
        stackId = cloudFormationClient.updateStack(updateStackRequest).stackId();
        System.err.println("Stack update begun.");

      } else if  (lastEvent.resourceStatus() == ResourceStatus.CREATE_FAILED
          && parameters.isResolveFailedCreate()
      ) {
        System.err.println("Previous stack failed to create, deleting now.");
        DeleteStackRequest deleteStackRequest = DeleteStackRequest.builder()
            .stackName(source.getName()).build();
        cloudFormationClient.deleteStack(deleteStackRequest);
        System.err.println("Deletion started.");

        return upsert(requestToken, source, parameters);
      } else {
        throw new IllegalStateException("Stack is not updatable, it is " + lastEvent.resourceStatus());
      }
    } else {
      System.err.println("Stack does not exist, creating it.");

      CreateStackRequest createStackRequest = CreateStackRequest.builder()
          .stackName(source.getName())
          .templateBody(parameters.getTemplate())
          .tags(parameters.getTags())
          .capabilities(parameters.getCapabilities())
          .notificationARNs(source.getNotificationArns())
          .clientRequestToken(requestToken)
          .build();
     stackId = cloudFormationClient.createStack(createStackRequest).stackId();
      System.err.println("Stack creation begun.");
    }

    return new EventTailer(cloudFormationClient, stackId, requestToken);

  }



  private Optional<StackEvent> getLastEvent(String stackName) {
    DescribeStackEventsRequest describeStackEventsRequest = DescribeStackEventsRequest
        .builder()
        .stackName(stackName)
        .build();
    DescribeStackEventsResponse describeStackEventsResponse;
    try {
      describeStackEventsResponse = cloudFormationClient
          .describeStackEvents(describeStackEventsRequest);
    } catch (CloudFormationException e) {
      if (e.awsErrorDetails().errorCode().equals("400")
          && e.awsErrorDetails().errorMessage()
          .equals(String.format("Stack [%s] does not exist", stackName))) {
        return Optional.empty();
      }
      throw e;
    }
    if (!describeStackEventsResponse.hasStackEvents() || describeStackEventsResponse.stackEvents()
        .isEmpty()) {
      throw new IllegalStateException(
          "No stack events were found for valid stack, should never happen!");
    }
   return Optional.of(describeStackEventsResponse.stackEvents().get(0));

  }

  public static boolean isTerminatingEvent(String stackName, StackEvent stackEvent) {
    if ("AWS::CloudFormation::Stack".equals(stackEvent.resourceType())
        && stackEvent.logicalResourceId().equals(stackName)
        && stackEvent.stackName().equals(stackName)
        && stackEvent.stackId().equals(stackEvent.physicalResourceId())
    ) {
      switch (stackEvent.resourceStatus()) {
        case UPDATE_COMPLETE:
        case CREATE_COMPLETE:
        case DELETE_FAILED:
        case CREATE_FAILED:
        case UPDATE_FAILED:
        case DELETE_COMPLETE:
        case IMPORT_COMPLETE:
        case IMPORT_ROLLBACK_COMPLETE:
        case IMPORT_FAILED:
          return true;
        default:
          return false;
      }
    } else {
      return false;
    }
  }
}
