package nz.co.eroad.concourse.resource.cloudformation;

import software.amazon.awssdk.services.cloudformation.model.StackEvent;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class EventType {



  private final static Set<StackStatus> UPDATABLE_EVENTS = new HashSet<>(Arrays.asList(
      StackStatus.CREATE_COMPLETE,
      StackStatus.UPDATE_COMPLETE,
      StackStatus.UPDATE_ROLLBACK_COMPLETE,
      StackStatus.IMPORT_COMPLETE
  ));

  private final static Set<StackStatus> FAILED_CREATE_EVENTS = new HashSet<>(Arrays.asList(
      StackStatus.CREATE_FAILED,
      StackStatus.ROLLBACK_FAILED,
      StackStatus.ROLLBACK_COMPLETE,
      StackStatus.IMPORT_ROLLBACK_FAILED,
      StackStatus.IMPORT_ROLLBACK_COMPLETE
  ));

  private final static Set<StackStatus> TERMINATING_EVENTS = new HashSet<>(Arrays.asList(
      StackStatus.CREATE_COMPLETE,
      StackStatus.CREATE_FAILED,
      StackStatus.UPDATE_COMPLETE,
      StackStatus.ROLLBACK_COMPLETE,
      StackStatus.ROLLBACK_FAILED,
      StackStatus.UPDATE_ROLLBACK_COMPLETE,
      StackStatus.UPDATE_ROLLBACK_FAILED,
      StackStatus.DELETE_COMPLETE,
      StackStatus.DELETE_FAILED,
      StackStatus.IMPORT_COMPLETE,
      StackStatus.IMPORT_ROLLBACK_COMPLETE,
      StackStatus.IMPORT_ROLLBACK_FAILED
      ));

  private final static Set<StackStatus> SUCCESS_EVENTS = new HashSet<>(Arrays.asList(
      StackStatus.CREATE_COMPLETE,
      StackStatus.UPDATE_COMPLETE
  ));

  private final static Set<StackStatus> STARTING_EVENTS = new HashSet<>(Arrays.asList(
      StackStatus.CREATE_IN_PROGRESS,
      StackStatus.UPDATE_IN_PROGRESS
      ));

  private final static Set<StackStatus> ROLLBACK_INITIATED_EVENTS = new HashSet<>(Arrays.asList(
      StackStatus.ROLLBACK_IN_PROGRESS,
      StackStatus.UPDATE_ROLLBACK_IN_PROGRESS,
      StackStatus.IMPORT_ROLLBACK_IN_PROGRESS
  ));

  private final static Set<StackStatus> USER_INITIATED_ROLLBACK_EVENTS = new HashSet<>(Arrays.asList(
      StackStatus.ROLLBACK_IN_PROGRESS,
      StackStatus.DELETE_IN_PROGRESS
  ));


  public static boolean isFailedCreateStack(StackStatus stackStatus) {
    return FAILED_CREATE_EVENTS.contains(stackStatus);
  }

  public static boolean isStableStack(String stackName, StackEvent stackEvent) {
    return isStackEvent(stackName, stackEvent)
        && TERMINATING_EVENTS.contains(StackStatus.fromValue(stackEvent.resourceStatusAsString()));
  }

  public static boolean isStableStack(StackStatus stackStatus) {
    return TERMINATING_EVENTS.contains(stackStatus);
  }



  public static boolean isExistingStack(StackStatus stackStatus) {
    return UPDATABLE_EVENTS.contains(stackStatus);
  }


  public static boolean isSuccessEvent(String stackName, StackEvent stackEvent) {
    return isStackEvent(stackName, stackEvent)
        && SUCCESS_EVENTS.contains(StackStatus.fromValue(stackEvent.resourceStatusAsString()));
  }
  public static boolean isStartingEvent(String stackName, StackEvent stackEvent) {

    return isStackEvent(stackName, stackEvent)
        && "User Initiated".equals(stackEvent.resourceStatusReason())
        && STARTING_EVENTS.contains(StackStatus.fromValue(stackEvent.resourceStatusAsString()));
  }

  public static boolean isUserInitiatedRollback(String stackName, StackEvent stackEvent) {
    return isStackEvent(stackName, stackEvent)
            && "User Initiated".equals(stackEvent.resourceStatusReason())
            && USER_INITIATED_ROLLBACK_EVENTS.contains(StackStatus.fromValue(stackEvent.resourceStatusAsString()));
  }

  public static boolean isDeletedStack(StackStatus stackStatus) {
    return stackStatus == StackStatus.DELETE_COMPLETE;
  }

  private static boolean isStackEvent(String stackName, StackEvent stackEvent) {
    return "AWS::CloudFormation::Stack".equals(stackEvent.resourceType())
        && stackEvent.logicalResourceId().equals(stackName)
        && stackEvent.stackName().equals(stackName)
        && stackEvent.stackId().equals(stackEvent.physicalResourceId());
  }

  public static boolean isRollingBack(String stackName, StackEvent stackEvent) {
    return isStackEvent(stackName, stackEvent)
        && ROLLBACK_INITIATED_EVENTS.contains(StackStatus.fromValue(stackEvent.resourceStatusAsString()));
  }

}

