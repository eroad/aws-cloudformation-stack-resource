package nz.co.eroad.concourse.resource.cloudformation;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.inject.Singleton;
import nz.co.eroad.concourse.resource.cloudformation.impl.CloudformationOperations;
import software.amazon.awssdk.services.cloudformation.model.Stack;

@Singleton
public class Check {
  
  public List<Version> run(Source source, Version existing) {
    CloudformationOperations cloudformationOperations = new CloudformationOperations(
        source.getRegion(), source.getCredentials()
    );
    Optional<Stack> found = cloudformationOperations.getStack(source.getName());
    if (found.isEmpty()) {
      return Collections.emptyList();
    } else {
      Stack stack = found.get();
      switch (stack.stackStatus()) {
        case CREATE_COMPLETE:
        case UPDATE_COMPLETE:
        case ROLLBACK_COMPLETE:
          Version newVersion = Version.fromStack(stack);
          return Collections.singletonList(newVersion);
        case DELETE_COMPLETE:
        case CREATE_FAILED:
          return Collections.emptyList();
        default:
          if (existing == null) {
            return Collections.emptyList();
          } else {
            return Collections.singletonList(existing);
          }
      }
    }
  }

}
