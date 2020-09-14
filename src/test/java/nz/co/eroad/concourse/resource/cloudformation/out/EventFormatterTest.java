package nz.co.eroad.concourse.resource.cloudformation.out;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cloudformation.model.ResourceStatus;
import software.amazon.awssdk.services.cloudformation.model.StackEvent;

public class EventFormatterTest {


  @Test
  public void testEventFormatter() {
    StackEvent event = StackEvent.builder().timestamp(Instant.MAX)
        .resourceType("EROAD::FAKE::RESOURCE")
        .logicalResourceId("fake-1234").resourceStatus(
            ResourceStatus.UPDATE_IN_PROGRESS).resourceStatusReason("potato").build();
    String potato = EventFormatter.format(event, true);
    System.out.println(potato);


  }
}
