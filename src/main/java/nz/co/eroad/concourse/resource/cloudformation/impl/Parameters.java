package nz.co.eroad.concourse.resource.cloudformation.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import software.amazon.awssdk.services.cloudformation.model.Capability;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.Tag;

public class Parameters {

  private final static ObjectMapper objectMapper = new ObjectMapper();
  private final List<Parameter> parameters;
  private final List<Tag> tags;
  private final String template;
  private final List<Capability> capabilities;
  private final boolean resolveFailedCreate;

  Parameters(
      List<Parameter> parameters,
      List<Tag> tags, String template,
      List<Capability> capabilities, boolean resolveFailedCreate) {
    this.parameters = parameters;
    this.tags = tags;
    this.template = template;
    this.capabilities = capabilities;
    this.resolveFailedCreate = resolveFailedCreate;
  }

  public List<Parameter> getParameters() {
    return parameters;
  }

  public List<Tag> getTags() {
    return tags;
  }

  public String getTemplate() {
    return template;
  }

  public List<Capability> getCapabilities() {
    return capabilities;
  }

  public boolean isResolveFailedCreate() {
    return resolveFailedCreate;
  }


}
