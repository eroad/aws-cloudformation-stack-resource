package nz.co.eroad.concourse.resource.cloudformation.out;

import static nz.co.eroad.concourse.resource.cloudformation.out.EventType.isStartingEvent;
import static nz.co.eroad.concourse.resource.cloudformation.out.EventType.isStableStack;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Stack;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest;
import software.amazon.awssdk.services.cloudformation.model.StackEvent;

class EventTailer implements Iterator<StackEvent> {


  private final CloudFormationClient cloudFormationClient;
  private final String stackId;
  private final String requestToken;
  private final String stackName;

  private StackEvent lastEvent;

  private Instant nextUpdate = Instant.now();
  private final Stack<StackEvent> stackEvents = new Stack<>();

  EventTailer(CloudFormationClient cloudFormationClient, String stackId, String requestToken) {
    this.stackName = nameFromId(stackId);
    this.cloudFormationClient = cloudFormationClient;
    this.stackId = stackId;
    this.requestToken = requestToken;
  }

  @Override
  public boolean hasNext() {
    return lastEvent == null || !isStableStack(nameFromId(stackId), lastEvent);
  }

  @Override
  public StackEvent next() {
    if (!hasNext()) {
      return null;
    }
    while (stackEvents.isEmpty()) {
      Duration toSleep = Duration.between(Instant.now(), nextUpdate);
      try {
        Thread.sleep(toSleep.toMillis());
        populateStack(lastEvent);
        nextUpdate = Instant.now().plus(10, ChronoUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
    }
    this.lastEvent = stackEvents.pop();
    return lastEvent;
  }


  private String nameFromId(String stackId) {
    String[] split = stackId.split("/");
    return split[1];
  }

  private void populateStack(StackEvent lastEvent) {


    DescribeStackEventsRequest describeStackEventsRequest = DescribeStackEventsRequest
        .builder()
        .stackName(stackId)
        .build();

    for (StackEvent next : cloudFormationClient.describeStackEventsPaginator(describeStackEventsRequest).stackEvents()) {
      if (next.equals(lastEvent)) {
        break;
      } else if (next.clientRequestToken().equals(requestToken)) {
        stackEvents.add(next);
        if(isStartingEvent(stackName, next)) {
          break;
        }
      }
    }


  }



}
