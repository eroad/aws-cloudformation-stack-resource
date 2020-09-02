package nz.co.eroad.concourse.resource.cloudformation.in;

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
import nz.co.eroad.concourse.resource.cloudformation.pojo.Metadata;
import nz.co.eroad.concourse.resource.cloudformation.pojo.Version;
import nz.co.eroad.concourse.resource.cloudformation.pojo.VersionMetadata;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.Output;
import software.amazon.awssdk.services.cloudformation.model.Stack;

public class In {

  private final CloudFormationClient cloudFormationClient;
  private final ObjectMapper objectMapper;

  public In(CloudFormationClient cloudFormationClient, ObjectMapper objectMapper) {
    this.cloudFormationClient = cloudFormationClient;
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

  public VersionMetadata run(String workingDirectory, Version existing) {

    DescribeStacksResponse describeStacksResponse = cloudFormationClient.describeStacks(
        builder -> builder.stackName(existing.getArn())
    );
    if (describeStacksResponse.stacks() == null || !describeStacksResponse.hasStacks() || describeStacksResponse.stacks().isEmpty()) {
      throw new IllegalStateException("Stack no longer exists!");
    }
    Stack found = describeStacksResponse.stacks().get(0);
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


