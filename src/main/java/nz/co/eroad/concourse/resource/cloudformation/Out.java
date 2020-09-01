package nz.co.eroad.concourse.resource.cloudformation;

import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import nz.co.eroad.concourse.resource.cloudformation.impl.CloudformationOperations;
import nz.co.eroad.concourse.resource.cloudformation.impl.StackParametersParser;
import nz.co.eroad.concourse.resource.cloudformation.impl.Parameters;
import software.amazon.awssdk.services.cloudformation.model.ResourceStatus;
import software.amazon.awssdk.services.cloudformation.model.StackEvent;

@Singleton
public class Out {

  private final StackParametersParser parametersParser;

  @Inject
  public Out(StackParametersParser parametersParser) {
    this.parametersParser = parametersParser;
  }
  
  public VersionMetadata run(String workingDirectory, Source source, Params params) {
    Parameters parameters = parametersParser.load(workingDirectory, params);
    CloudformationOperations cloudformationOperations = new CloudformationOperations(
        source.getRegion(), source.getCredentials()
    );
    String clientRequestToken = UUID.randomUUID().toString();

    Iterator<StackEvent> stackEvents = cloudformationOperations.upsert(clientRequestToken, source, parameters);
    if (!stackEvents.hasNext()) {
      throw new IllegalStateException("Should have provided at least one stack event after update!");
    }
    StackEvent first = stackEvents.next();
    StackEvent last = first;
    System.err.println(last.toString());
    while (stackEvents.hasNext()) {
      last = stackEvents.next();
      System.err.println(last.toString());
    }

    if (last.resourceStatus() != ResourceStatus.CREATE_COMPLETE &&
        last.resourceStatus() != ResourceStatus.UPDATE_COMPLETE) {
      throw new IllegalStateException(
          "Stack failed to create or update: " + last.resourceStatusReason());
    }


    Version version = new Version(first.physicalResourceId(), first.timestamp());
    Metadata status = new Metadata("status", last.resourceStatusAsString());
    return new VersionMetadata(
        version,
        Collections.singletonList(status)
    );
  }

}
