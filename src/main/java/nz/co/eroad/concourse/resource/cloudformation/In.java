package nz.co.eroad.concourse.resource.cloudformation;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;
import nz.co.eroad.concourse.resource.cloudformation.impl.CloudformationOperations;
import software.amazon.awssdk.services.cloudformation.model.Output;
import software.amazon.awssdk.services.cloudformation.model.Stack;

@Singleton
public class In {
  
  private final ObjectMapper objectMapper;

  @Inject
  In(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  private void populateFiles(String directory, Stack stack) {

    try {
      Files.write(Paths.get(directory, "name"), stack.stackName().getBytes(StandardCharsets.UTF_8));
      Files.write(Paths.get(directory, "arn"), stack.stackId().getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    writeOutputsToJson(
        Paths.get(directory, "outputs.json"),
        stack.outputs()
    );

  }

  private void writeOutputsToJson(Path path, List<Output> outputs) {
    Map<String, String> outputsMap = outputs.stream()
        .collect(Collectors.toMap(Output::outputKey, Output::outputValue));
    try {
      objectMapper.writeValue(path.toFile(), outputsMap);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  public VersionMetadata run(String workingDirectory, Source source, Version existing) {

    CloudformationOperations cloudformationOperations = new CloudformationOperations(
        source.getRegion(), source.getCredentials()
    );

    Stack found = cloudformationOperations.getStack(existing.getArn()).orElseThrow(() -> new IllegalArgumentException("Stack with arn " + existing.getArn() + " does not exist!"));
    switch (found.stackStatus()) {
      case CREATE_COMPLETE:
      case UPDATE_COMPLETE:
      case ROLLBACK_COMPLETE:
        Version version = Version.fromStack(found);
        if (!existing.equals(version)) {
          throw new IllegalStateException("Cannot get stack info, stack has been updated concurrently");
        }
        populateFiles(workingDirectory, found);
        Metadata status = new Metadata("status", found.stackStatusAsString());
        return new VersionMetadata(version, Collections.singletonList(status));
      default:
        throw new IllegalStateException("Cannot get stack info, current state is " + found.stackStatus().toString());
    }

  }
}


