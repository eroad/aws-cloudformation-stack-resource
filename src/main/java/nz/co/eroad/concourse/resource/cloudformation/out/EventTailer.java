package nz.co.eroad.concourse.resource.cloudformation.out;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest;
import software.amazon.awssdk.services.cloudformation.model.StackEvent;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

import static nz.co.eroad.concourse.resource.cloudformation.EventType.*;

class EventTailer implements Iterator<StackEvent> {


  private final CloudFormationClient cloudFormationClient;
  private final String stackId;
  private final String originalRequestToken;
  private String currentRequestToken;
  private final String stackName;

  private StackEvent lastEvent;

  private Instant nextUpdate = Instant.now();
  private final Queue<StackEvent> stackEvents = new ArrayDeque<>();

  EventTailer(CloudFormationClient cloudFormationClient, String stackId, String requestToken) {
    this.stackName = nameFromId(stackId);
    this.cloudFormationClient = cloudFormationClient;
    this.stackId = stackId;
    this.originalRequestToken = requestToken;
    this.currentRequestToken = requestToken;
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
    this.lastEvent = stackEvents.poll();
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

    ArrayDeque<StackEvent> unseenEventsStack = new ArrayDeque<>();

    for (StackEvent next : cloudFormationClient.describeStackEventsPaginator(describeStackEventsRequest).stackEvents()) {
      if (next.equals(lastEvent)) { //at end of un-seen events
        break;
      }

      unseenEventsStack.addFirst(next);

      if(isStartingEvent(stackName, next) && originalRequestToken.contains(next.clientRequestToken())) { //at beginning of this stack update
        break;
      }
    }



    StackEvent oldestSeen = lastEvent;
    for (StackEvent oldestUnseen : unseenEventsStack) {

      // Request token changes during user initiated rollback. Only catch user initiated rollbacks during updates.
      if (oldestSeen != null //make sure this happened in progress
              && oldestSeen.clientRequestToken().equals(currentRequestToken)
              && !isStableStack(stackName, oldestSeen) //ignore 'continue rollback' request
              && isUserInitiatedRollback(stackName, oldestUnseen)
      ) {
        currentRequestToken = oldestUnseen.clientRequestToken();
      }
      if (!currentRequestToken.equals(oldestUnseen.clientRequestToken())) { //no longer belongs to this stack update request
        break;
      }
      stackEvents.add(oldestUnseen);
      oldestSeen = oldestUnseen;
    }

  }



}
