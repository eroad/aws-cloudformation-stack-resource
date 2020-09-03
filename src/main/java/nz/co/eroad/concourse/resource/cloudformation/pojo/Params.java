package nz.co.eroad.concourse.resource.cloudformation.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;

public class Params {


  private final List<String> capabilities;
  private final String parametersFile;
  private final String preformattedParametersFile;
  private final String templateFile;
  private final String tagsFile;
  private final boolean resolveFailedCreate;
  private final String s3Bucket;

  @JsonCreator
  public Params(
      @JsonProperty("parameters") String parametersFile,
      @JsonProperty("parameters_aws") String preformattedParametersFile,
      @JsonProperty(value = "template", required = true) String templateFile,
      @JsonProperty("tags") String tagsFile,
      @JsonProperty("capabilities") List<String> capabilities,
      @JsonProperty("resolve_failed_create") Boolean resolveFailedCreate,
      @JsonProperty("s3_bucket") String s3Bucket) {

    this.preformattedParametersFile = nullIfBlank(preformattedParametersFile);
    this.parametersFile = nullIfBlank(parametersFile);

    this.tagsFile = nullIfBlank(tagsFile);
    this.s3Bucket = nullIfBlank(s3Bucket);

    if (templateFile == null || templateFile.isBlank()) {
      throw new IllegalArgumentException("'template' parameters must be specified");
    }
    this.templateFile = templateFile;
    this.capabilities = capabilities == null ? Collections.emptyList() : capabilities;
    this.resolveFailedCreate = Boolean.TRUE.equals(resolveFailedCreate);
  }

  public List<String> getCapabilities() {
    return capabilities;
  }

  public String getParametersFile() {
    return parametersFile;
  }

  public String getPreformattedParametersFile() {
    return preformattedParametersFile;
  }

  public String getTemplateFile() {
    return templateFile;
  }

  public String getTagsFile() {
    return tagsFile;
  }

  public Boolean getResolveFailedCreate() {
    return resolveFailedCreate;
  }
  
  private static String nullIfBlank(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value;
  }

  public String getS3Bucket() {
    return s3Bucket;
  }
}
