package nz.co.eroad.concourse.resource.cloudformation;

import java.util.List;
import software.amazon.awssdk.services.cloudformation.model.Capability;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.Tag;

public class ParsedFiles {

  private final List<Parameter> parameters;
  private final List<Tag> tags;
  private final List<Capability> capabilities;
  private final String templateBody;


  public ParsedFiles(
      List<Parameter> parameters,
      List<Tag> tags,
      List<Capability> capabilities,
      String templateBody) {
    this.parameters = parameters;
    this.tags = tags;
    this.capabilities = capabilities;
    this.templateBody = templateBody;
  }

  public List<Parameter> getParameters() {
    return parameters;
  }

  public List<Tag> getTags() {
    return tags;
  }

  public String getTemplateBody() {
    return templateBody;
  }

  public List<Capability> getCapabilities() {
    return capabilities;
  }



}
