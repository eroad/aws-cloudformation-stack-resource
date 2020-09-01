package nz.co.eroad.concourse.resource.cloudformation.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Singleton;
import nz.co.eroad.concourse.resource.cloudformation.Params;
import software.amazon.awssdk.services.cloudformation.model.Capability;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.Tag;

@Singleton
public class StackParametersParser {

  private final ObjectMapper objectMapper;

  public StackParametersParser(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Parameters load(String workingDirectory, Params params) {
    if (workingDirectory == null || params == null) {
      throw new IllegalArgumentException("Base directory and params must be provided!");
    }



    List<Parameter> parameters = new ArrayList<>();
    String parametersFile = params.getParametersFile();
    if (parametersFile != null) {
      parameters.addAll(readMapParameters(Paths.get(workingDirectory, parametersFile)));
    }

    String parametersAwsFile = params.getPreformattedParametersFile();
    if (parametersAwsFile != null) {
      parameters.addAll(readPreformattedParameters(Paths.get(workingDirectory, parametersAwsFile)));
    }


    String templateBody = readTemplateBody(Paths.get(workingDirectory, params.getTemplateFile()));

    List<Tag> tags = Collections.emptyList();
    String tagsFile = params.getTagsFile();
    if (tagsFile != null && !tagsFile.isBlank()) {
      tags = readTags(Paths.get(workingDirectory, tagsFile));
    }

    List<Capability> capabilities = toAwsCapabilities(params.getCapabilities());

    boolean resolveFailedCreate = params.getResolveFailedCreate();
    return new Parameters(parameters, tags, templateBody, capabilities, resolveFailedCreate);
  }

  private List<Capability> toAwsCapabilities(List<String> toCapabilities) {
    return toCapabilities.stream()
        .map(Capability::fromValue)
        .collect(Collectors.toList());
  }

  private List<Tag> readTags(Path path) {
    List<Tag.Builder> builders = null;
    try {
      builders = objectMapper.readValue(
          path.toFile(), objectMapper.getTypeFactory()
              .constructCollectionLikeType(List.class, Tag.serializableBuilderClass())
      );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return builders.stream()
        .map(Tag.Builder::build)
        .collect(Collectors.toList());
  }

  private List<Parameter> readMapParameters(Path path) {

    try {
      return objectMapper.readValue(
          path.toFile(), new TypeReference<Map<String, String>>() {}
      ).entrySet()
          .stream()
          .map(entry -> Parameter.builder()
              .parameterKey(entry.getKey())
              .parameterValue(entry.getValue())
              .build())
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private List<Parameter> readPreformattedParameters(Path path) {

    try {
      List<Parameter.Builder> parameterBuilders = objectMapper.readValue(
          path.toFile(), objectMapper.getTypeFactory()
              .constructCollectionType(List.class, Parameter.serializableBuilderClass())
      );
      return parameterBuilders.stream().map(Parameter.Builder::build).collect(Collectors.toList());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static String readTemplateBody(Path path) {
    try {
      return Files.readString(path, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
