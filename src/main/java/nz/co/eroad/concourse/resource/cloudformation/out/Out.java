package nz.co.eroad.concourse.resource.cloudformation.out;

import static nz.co.eroad.concourse.resource.cloudformation.out.EventType.isTerminatingEvent;

import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;
import nz.co.eroad.concourse.resource.cloudformation.ParsedFiles;
import nz.co.eroad.concourse.resource.cloudformation.StackOptionsFilesParser;
import nz.co.eroad.concourse.resource.cloudformation.pojo.Metadata;
import nz.co.eroad.concourse.resource.cloudformation.pojo.Params;
import nz.co.eroad.concourse.resource.cloudformation.pojo.Source;
import nz.co.eroad.concourse.resource.cloudformation.pojo.Version;
import nz.co.eroad.concourse.resource.cloudformation.pojo.VersionMetadata;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest.Builder;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsResponse;
import software.amazon.awssdk.services.cloudformation.model.StackEvent;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class Out {

  private final StackOptionsFilesParser parametersParser;
  private final CloudFormationClient cloudFormationClient;
  private final S3Client s3Client;

  public Out(StackOptionsFilesParser parametersParser, CloudFormationClient cloudFormationClient, S3Client s3Client) {
    this.parametersParser = parametersParser;
    this.cloudFormationClient = cloudFormationClient;
    this.s3Client = s3Client;
  }

  private String uploadTemplate(String bucket, String stackName, String template) {
    String s3Key = stackName + "/" + Instant.now().toString().replace(":", "-");
    PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket(bucket).key(s3Key).build();
    s3Client.putObject(putObjectRequest, RequestBody.fromString(template));
    return s3Client.utilities().getUrl(GetUrlRequest.builder().bucket(bucket).key(s3Key).build()).toExternalForm();
  }
  
  public VersionMetadata run(String workingDirectory, Source source, Params params) {
    System.err.println("Reticulating splines.");
    ParsedFiles parsedFiles = parametersParser.load(workingDirectory, params);

    String templateUrl = null;
    if (params.getS3Bucket() != null) {
      templateUrl = uploadTemplate(params.getS3Bucket(), source.getName(), parsedFiles.getTemplateBody());
    } else if (parsedFiles.getTemplateBody().length() > 51200) {
      throw new IllegalArgumentException("Template body is too large to directly use, please specify an s3_bucket to upload it too as part of deploy.");
    }

    Optional<StackEvent> lastUpdateEvent = awaitStackStable(source.getName());
    if (lastUpdateEvent.isPresent() && EventType.isFailedCreateEvent(source.getName(), lastUpdateEvent.get())) {
      if (Boolean.TRUE.equals(params.getResolveFailedCreate())) {
        System.err.println("Previous stack failed to create, deleting now.");
        DeleteStackRequest deleteStackRequest = DeleteStackRequest.builder()
            .stackName(source.getName()).build();
        cloudFormationClient.deleteStack(deleteStackRequest);
      }
      lastUpdateEvent = awaitStackStable(source.getName());
    }
    lastUpdateEvent.ifPresent(event -> System.err.println("Current stack state is " + event.resourceStatusAsString()));


    String requstToken = UUID.randomUUID().toString();
    String stackId;
    if (lastUpdateEvent.isEmpty() || EventType.isCreatableFrom(source.getName(), lastUpdateEvent.get())) {
      stackId = createStack(requstToken, source, parsedFiles, templateUrl);
    } else if (EventType.isUpdatableEvent(source.getName(), lastUpdateEvent.get())) {
      try {
        stackId = updateStack(requstToken, source, parsedFiles, templateUrl);
      } catch (CloudFormationException e) {
        if (e.awsErrorDetails().errorCode().equals("ValidationError")
            && e.awsErrorDetails().errorMessage()
            .equals("No updates are to be performed.")) {
          System.err.println("No updates are needed.");

          Version version = new Version(lastUpdateEvent.get().physicalResourceId(), lastUpdateEvent.get().timestamp());
          Metadata status = new Metadata("status", lastUpdateEvent.get().resourceStatusAsString());

          return new VersionMetadata(
              version,
              Collections.singletonList(status)
          );
        }
        throw e;
      }
    } else {
      throw new IllegalStateException("Stack is not updatable because it is currently in a state of " + lastUpdateEvent.get().resourceStatusAsString());
    }

    Iterator<StackEvent> stackEvents = new EventTailer(cloudFormationClient, stackId, requstToken);
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


  private Optional<StackEvent> awaitStackStable(String stackName)  {
    Optional<StackEvent> lastEvent = getLastEvent(stackName);
    if (lastEvent.isPresent() && !isTerminatingEvent(stackName, lastEvent.get())) {
      System.err.println("An update is in progress. Please stand by.");
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
    System.err.println("Stack creation begun.");
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
    System.err.println("Stack update begun. ");
    return stackId;
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
      if (e.awsErrorDetails().errorCode().equals("ValidationError")
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



}
