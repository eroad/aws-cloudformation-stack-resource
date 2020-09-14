package nz.co.eroad.concourse.resource.cloudformation.out;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import software.amazon.awssdk.services.cloudformation.model.ResourceStatus;
import software.amazon.awssdk.services.cloudformation.model.StackEvent;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;

class EventType {

  private final static Set<String> BAD_EVENTS = new HashSet<>(Arrays.asList(
      ResourceStatus.UPDATE_FAILED.toString(),
      ResourceStatus.CREATE_FAILED.toString(),
      ResourceStatus.DELETE_FAILED.toString(),
      ResourceStatus.IMPORT_FAILED.toString(),
      ResourceStatus.IMPORT_ROLLBACK_FAILED.toString(),
      StackStatus.UPDATE_ROLLBACK_FAILED.toString(),
      StackStatus.ROLLBACK_FAILED.toString(),
      StackStatus.CREATE_FAILED.toString(),
      StackStatus.IMPORT_ROLLBACK_FAILED.toString(),
      StackStatus.DELETE_FAILED.toString()
  ));
  private final static Set<String> WARNING_EVENTS = new HashSet<>(Arrays.asList(
      StackStatus.UPDATE_ROLLBACK_IN_PROGRESS.toString(),
      StackStatus.UPDATE_ROLLBACK_COMPLETE.toString(),
      StackStatus.IMPORT_ROLLBACK_IN_PROGRESS.toString(),
      StackStatus.IMPORT_ROLLBACK_COMPLETE.toString(),
      StackStatus.ROLLBACK_COMPLETE.toString(),
      StackStatus.ROLLBACK_IN_PROGRESS.toString()
  ));

  private final static Set<StackStatus> UPDATABLE_EVENTS = new HashSet<>(Arrays.asList(
      StackStatus.CREATE_COMPLETE,
      StackStatus.UPDATE_COMPLETE,
      StackStatus.UPDATE_ROLLBACK_COMPLETE,
      StackStatus.IMPORT_COMPLETE
  ));

  private final static Set<StackStatus> FAILED_CREATE_EVENTS = new HashSet<>(Arrays.asList(
      StackStatus.CREATE_FAILED,
      StackStatus.ROLLBACK_FAILED,
      StackStatus.ROLLBACK_COMPLETE
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


  static boolean isFailedCreateStack(StackStatus stackStatus) {
    return FAILED_CREATE_EVENTS.contains(stackStatus);
  }

  static boolean isStableStack(String stackName, StackEvent stackEvent) {
    return isStackEvent(stackName, stackEvent)
        && TERMINATING_EVENTS.contains(StackStatus.fromValue(stackEvent.resourceStatusAsString()));
  }

  static boolean isStableStack(StackStatus stackStatus) {
    return TERMINATING_EVENTS.contains(stackStatus);
  }



  static boolean isExistingStack(StackStatus stackStatus) {
    return UPDATABLE_EVENTS.contains(stackStatus);
  }


  static boolean isSuccessEvent(String stackName, StackEvent stackEvent) {
    return isStackEvent(stackName, stackEvent)
        && SUCCESS_EVENTS.contains(StackStatus.fromValue(stackEvent.resourceStatusAsString()));
  }
  static boolean isStartingEvent(String stackName, StackEvent stackEvent) {

    return isStackEvent(stackName, stackEvent)
        && "User Initiated".equals(stackEvent.resourceStatusReason())
        && STARTING_EVENTS.contains(StackStatus.fromValue(stackEvent.resourceStatusAsString()));
  }

  static boolean isDeletedStack(StackStatus stackStatus) {
    return stackStatus == StackStatus.DELETE_COMPLETE;
  }

  private static boolean isStackEvent(String stackName, StackEvent stackEvent) {
    return "AWS::CloudFormation::Stack".equals(stackEvent.resourceType())
        && stackEvent.logicalResourceId().equals(stackName)
        && stackEvent.stackName().equals(stackName)
        && stackEvent.stackId().equals(stackEvent.physicalResourceId());
  }

  static boolean isBadEvent(String status) {
    return BAD_EVENTS.contains(status);
  }

  static boolean isWarningEvent(String status) {
    return WARNING_EVENTS.contains(status);
  }
}

