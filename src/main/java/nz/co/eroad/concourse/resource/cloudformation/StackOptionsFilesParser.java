package nz.co.eroad.concourse.resource.cloudformation;

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
import nz.co.eroad.concourse.resource.cloudformation.pojo.Params;
import software.amazon.awssdk.services.cloudformation.model.Capability;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.Tag;

public class StackOptionsFilesParser {

  private final ObjectMapper objectMapper;

  public StackOptionsFilesParser(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public ParsedFiles load(String workingDirectory, Params params) {
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
    return new ParsedFiles( parameters, tags, capabilities, templateBody);
  }

  private List<Capability> toAwsCapabilities(List<String> toCapabilities) {
    return toCapabilities.stream()
        .map(Capability::fromValue)
        .collect(Collectors.toList());
  }

  private List<Tag> readTags(Path path) {
    List<nz.co.eroad.concourse.resource.cloudformation.pojo.Tag> builders = null;
    try {
      builders = objectMapper.readValue(
          path.toFile(), new TypeReference<>() {
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return builders.stream()
        .map(tag -> Tag.builder().key(tag.getKey()).value(tag.getValue()).build())
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
      List<nz.co.eroad.concourse.resource.cloudformation.pojo.Parameter> parameterBuilders = objectMapper.readValue(
          path.toFile(), new TypeReference<>() {});
      return parameterBuilders.stream().map(parameter -> Parameter.builder().parameterValue(parameter.getParameterValue()).parameterKey(parameter.getParameterKey()).resolvedValue(parameter.getResolvedValue()).usePreviousValue(parameter.getUsePreviousValue()).build()).collect(Collectors.toList());
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
