package nz.co.eroad.concourse.resource.cloudformation.out;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import picocli.CommandLine.Help.Ansi.Style;
import software.amazon.awssdk.services.cloudformation.model.ResourceStatus;
import software.amazon.awssdk.services.cloudformation.model.StackEvent;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;

public class EventFormatter {


  
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

  private final static Set<String> GOOD_EVENTS = new HashSet<>(Arrays.asList(
      ResourceStatus.CREATE_COMPLETE.toString(),
      ResourceStatus.UPDATE_COMPLETE.toString(),
      ResourceStatus.DELETE_COMPLETE.toString(),
      ResourceStatus.IMPORT_COMPLETE.toString(),
      StackStatus.CREATE_COMPLETE.toString(),
      StackStatus.UPDATE_COMPLETE.toString(),
      StackStatus.ROLLBACK_COMPLETE.toString(),
      StackStatus.UPDATE_ROLLBACK_COMPLETE.toString(),
      StackStatus.IMPORT_COMPLETE.toString(),
      StackStatus.IMPORT_ROLLBACK_COMPLETE.toString(),
      StackStatus.DELETE_COMPLETE.toString()
  ));

  public static String format(StackEvent stackEvent, boolean rollbackInProgress) {
    String resourceStatus = stackEvent.resourceStatusAsString();
    String colors = "";
    if (rollbackInProgress) {
      colors = Style.bg_yellow.on();
    }
    if (GOOD_EVENTS.contains(resourceStatus)) {
      colors = colors + Style.fg_green.on();
    } else if (BAD_EVENTS.contains(resourceStatus)) {
      colors = colors + Style.fg_red.on();
    } else {
      colors = colors + Style.fg_white.on();
    }
    return colors + formatEvent(stackEvent) + Style.reset.on();

  }


  private static String formatEvent(StackEvent stackEvent) {
    return String.format("%s | %s | %s | %s", stackEvent.timestamp().toString(), stackEvent.resourceType(), stackEvent.logicalResourceId(),  stackEvent.resourceStatusReason() == null ? stackEvent.resourceStatusAsString() : stackEvent.resourceStatusAsString() + " | " + stackEvent.resourceStatusReason());
  }
}
