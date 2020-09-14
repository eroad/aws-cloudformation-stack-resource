package nz.co.eroad.concourse.resource.cloudformation.out;

import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import nz.co.eroad.concourse.resource.cloudformation.ParsedFiles;
import nz.co.eroad.concourse.resource.cloudformation.StackOptionsFilesParser;
import nz.co.eroad.concourse.resource.cloudformation.aws.AwsClientFactory;
import nz.co.eroad.concourse.resource.cloudformation.pojo.Metadata;
import nz.co.eroad.concourse.resource.cloudformation.pojo.Params;
import nz.co.eroad.concourse.resource.cloudformation.pojo.Source;
import nz.co.eroad.concourse.resource.cloudformation.pojo.Version;
import nz.co.eroad.concourse.resource.cloudformation.pojo.VersionMetadata;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest.Builder;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.StackEvent;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLocationResponse;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class Out {

  private final StackOptionsFilesParser parametersParser;
  private final Source source;
  private final CloudFormationClient cloudFormationClient;
  private final S3Client s3Client;

  public Out(StackOptionsFilesParser parametersParser, Source source) {
    this.parametersParser = parametersParser;
    this.source = source;
    this.cloudFormationClient = AwsClientFactory
        .cloudFormationClient(source.getRegion(), source.getCredentials());
    this.s3Client = AwsClientFactory.s3Client(
        source.getRegion(), source.getCredentials()
    );

  }

  private String uploadTemplate(String bucket, String stackName, String template) {
    GetBucketLocationResponse bucketLocationResponse = s3Client
        .getBucketLocation(GetBucketLocationRequest.builder().bucket(bucket).build());
    Region region = bucketLocationResponse.locationConstraintAsString() == null ? Region.US_EAST_1 : Region.of(bucketLocationResponse.locationConstraintAsString());
    S3Client regionalClient = AwsClientFactory.s3Client(region, source.getCredentials());

    String s3Key = stackName + "/" + Instant.now().toString().replace(":", "-");
    System.err.println("Uploading template to S3 in " + region + " to " + bucket + "/" + s3Key);
    PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket(bucket).key(s3Key).build();
    regionalClient.putObject(putObjectRequest, RequestBody.fromString(template));

    String url = regionalClient.utilities()
        .getUrl(GetUrlRequest.builder().bucket(bucket).key(s3Key).build()).toExternalForm();
    System.err.println("Template URL is " + url);
    return url;
  }
  
  public VersionMetadata run(String workingDirectory, Params params) {
    System.err.println("Reticulating splines.\n");
    ParsedFiles parsedFiles = parametersParser.load(workingDirectory, params);

    String templateUrl = null;
    if (params.getTemplateS3Bucket() != null) {
      templateUrl = uploadTemplate(params.getTemplateS3Bucket(), source.getName(), parsedFiles.getTemplateBody());
    } else if (parsedFiles.getTemplateBody().length() > 51200) {
      throw new IllegalArgumentException("Template body is too large to directly use, please specify an s3_bucket to upload it to.");
    }

    Optional<Stack> currentStack = awaitStackStable(source.getName());
    if (currentStack.isPresent() && EventType.isFailedCreateStack(currentStack.get().stackStatus())) {
      if (Boolean.TRUE.equals(params.getResolveFailedCreate())) {
        System.err.println("Previous stack failed to create, deleting now.");
        DeleteStackRequest deleteStackRequest = DeleteStackRequest.builder()
            .stackName(source.getName()).build();
        cloudFormationClient.deleteStack(deleteStackRequest);
      }
      currentStack = awaitStackStable(source.getName());
    }
    currentStack.ifPresent(event -> System.err.println("Current stack state is " + event.stackStatusAsString()));


    String requestToken = System.getenv("ATC_EXTERNAL_URL") + "/builds/" + System.getenv("BUILD_ID");
    String stackId;
    if (currentStack.isEmpty() || EventType.isDeletedStack( currentStack.get().stackStatus())) {
      stackId = createStack(requestToken, source, parsedFiles, templateUrl);
    } else if (EventType.isExistingStack(currentStack.get().stackStatus())) {
      try {
        stackId = updateStack(requestToken, source, parsedFiles, templateUrl);
      } catch (CloudFormationException e) {
        if (e.awsErrorDetails().errorCode().equals("ValidationError")
            && e.awsErrorDetails().errorMessage()
            .equals("No updates are to be performed.")) {
          System.err.println("No updates are needed.");

          Version version = Version.fromStack(currentStack.get());
          Metadata status = new Metadata("status", currentStack.get().stackStatusAsString());

          return new VersionMetadata(
              version,
              Collections.singletonList(status)
          );
        }
        throw e;
      }
    } else {
      throw new IllegalStateException("Stack is not updatable because it is currently in a state of " + currentStack.get().stackStatusAsString());
    }

    Iterator<StackEvent> stackEvents = new EventTailer(cloudFormationClient, stackId, requestToken);
    if (!stackEvents.hasNext()) {
      throw new IllegalStateException("Should have provided at least one stack event after update!");
    }
    StackEvent first = stackEvents.next();
    StackEvent last = first;
    printEvent(last);
    while (stackEvents.hasNext()) {
      last = stackEvents.next();
      printEvent(last);
    }

    if (!EventType.isSuccessEvent(source.getName(), last)) {
      throw new IllegalStateException(
          "Stack failed to create or update. It's state is now " + last.resourceStatusAsString());
    }


    Version version = new Version(first.physicalResourceId(), first.timestamp());
    Metadata status = new Metadata("status", last.resourceStatusAsString());
    return new VersionMetadata(
        version,
        Collections.singletonList(status)
    );
  }

  private void printEvent(StackEvent stackEvent) {
    System.err.printf("%s | %s | %s | %s %n", stackEvent.timestamp().toString(), stackEvent.resourceType(), stackEvent.logicalResourceId(),  stackEvent.resourceStatusReason() == null ? stackEvent.resourceStatusAsString() : stackEvent.resourceStatusAsString() + " | " + stackEvent.resourceStatusReason());

  }


  private Optional<Stack> awaitStackStable(String stackName)  {
    Optional<Stack> currentStack = getStack(stackName);
    if (currentStack.isPresent() && !EventType.isStableStack(currentStack.get().stackStatus())) {
      System.err.println("An update is in progress. Please stand by.");
      do {
        try {
          Thread.sleep(10000);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(e);
        }
        currentStack = getStack(stackName);
      } while (currentStack.isPresent() && !EventType.isStableStack(currentStack.get().stackStatus()));
    }
    return currentStack;

  }

  private String createStack(String requestToken, Source source, ParsedFiles parsedFiles, String templateUrl) {
    System.err.println("Stack does not exist, creating it.");

    Builder createStackRequest = CreateStackRequest.builder()
        .stackName(source.getName())
        .tags(parsedFiles.getTags())
        .capabilities(parsedFiles.getCapabilities())
        .notificationARNs(source.getNotificationArns())
        .clientRequestToken(requestToken)
        .parameters(parsedFiles.getParameters());
    if (templateUrl == null) {
      createStackRequest.templateBody(parsedFiles.getTemplateBody());
    } else {
      createStackRequest.templateURL(templateUrl);
    }
    String stackId = cloudFormationClient.createStack(createStackRequest.build()).stackId();
    System.err.println("Stack creation begun:\n");
    return stackId;
  }

  private String updateStack(String requestToken, Source source, ParsedFiles parsedFiles, String templateUrl) {
    System.err.println("Stack exists, updating it.");

    UpdateStackRequest.Builder updateStackRequest = UpdateStackRequest.builder()
        .stackName(source.getName())
        .capabilities(parsedFiles.getCapabilities())
        .parameters(parsedFiles.getParameters())
        .tags(parsedFiles.getTags())
        .notificationARNs(source.getNotificationArns())
        .clientRequestToken(requestToken);
    if (templateUrl == null) {
      updateStackRequest.templateBody(parsedFiles.getTemplateBody());
    } else {
      updateStackRequest.templateURL(templateUrl);
    }
    String stackId = cloudFormationClient.updateStack(updateStackRequest.build()).stackId();
    System.err.println("Stack update begun:\n");
    return stackId;
  }




  private Optional<Stack> getStack(String stackName) {
    DescribeStacksRequest describeStackEventsRequest = DescribeStacksRequest
        .builder()
        .stackName(stackName)
        .build();
    DescribeStacksResponse describeStackEventsResponse;
    try {
      describeStackEventsResponse = cloudFormationClient
          .describeStacks(describeStackEventsRequest);
    } catch (CloudFormationException e) {
      if (e.awsErrorDetails().errorCode().equals("ValidationError")
          && e.awsErrorDetails().errorMessage()
          .equals(String.format("Stack with id %s does not exist", stackName))) {
        return Optional.empty();
      }
      throw e;
    }
    if (!describeStackEventsResponse.hasStacks() || describeStackEventsResponse.stacks().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(describeStackEventsResponse.stacks().get(0));

  }



}
