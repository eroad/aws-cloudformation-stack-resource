package nz.co.eroad.concourse.resource.cloudformation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.picocli.runtime.PicocliCommandLineFactory;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import nz.co.eroad.concourse.resource.cloudformation.aws.AwsClientFactory;
import nz.co.eroad.concourse.resource.cloudformation.check.Check;
import nz.co.eroad.concourse.resource.cloudformation.in.In;
import nz.co.eroad.concourse.resource.cloudformation.out.Out;
import nz.co.eroad.concourse.resource.cloudformation.pojo.OutInput;
import nz.co.eroad.concourse.resource.cloudformation.pojo.Source;
import nz.co.eroad.concourse.resource.cloudformation.pojo.Version;
import nz.co.eroad.concourse.resource.cloudformation.pojo.VersionInput;
import nz.co.eroad.concourse.resource.cloudformation.pojo.VersionMetadata;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.s3.S3Client;

@picocli.CommandLine.Command(name = "concourse-resource")
public class ConcourseResourceCommand implements Runnable {


  @Spec
  private CommandSpec spec;


  private final ObjectMapper objectMapper;

  public ConcourseResourceCommand(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }



  @SuppressWarnings("unchecked")
  public void run() {
    throw new ParameterException(spec.commandLine(), "Specify a subcommand");
  } 




  @CommandLine.Command(name = "check")
  private void check() throws IOException, InterruptedException {
    VersionInput versionInput = objectMapper.readValue(
        System.in,
        VersionInput.class
    );
    if (versionInput == null) {
      throw new IllegalArgumentException("Input JSON was invalid!");
    }
    Source source = versionInput.getSource();
    CloudFormationClient cloudFormationClient = AwsClientFactory
        .cloudFormationClient(source.getRegion(), source.getCredentials());
    Check check = new Check(cloudFormationClient);
    List<Version> versions = check.run(versionInput.getSource().getName(), versionInput.getVersion());
    objectMapper.writeValue(new BufferedOutputStream(System.out), versions);
  }

  @CommandLine.Command(name = "in")
  private void in(@Parameters(arity = "1", description = "working directory") String workingDirectory) throws IOException {
    VersionInput versionInput = objectMapper.readValue(
        System.in,
        VersionInput.class
    );
    if (versionInput == null) {
      throw new IllegalArgumentException("Input JSON was invalid!");
    }
    Source source = versionInput.getSource();
    CloudFormationClient cloudFormationClient = AwsClientFactory
        .cloudFormationClient(source.getRegion(), source.getCredentials());
    In in = new In(cloudFormationClient, objectMapper);
    VersionMetadata versionMetadata = in.run(workingDirectory, versionInput.getVersion());
    objectMapper.writeValue(new BufferedOutputStream(System.out), versionMetadata);
  }

  @CommandLine.Command(name = "out")
  private void out(@Parameters(arity = "1",  description = "working directory") String workingDirectory) throws IOException {
    OutInput outInput = objectMapper.readValue(
        System.in,
        OutInput.class
    );
    if (outInput == null) {
      throw new IllegalArgumentException("Input JSON was invalid!");
    }
    Source source = outInput.getSource();
    CloudFormationClient cloudFormationClient = AwsClientFactory
        .cloudFormationClient(source.getRegion(), source.getCredentials());
    S3Client s3Client = AwsClientFactory.s3Client(source.getRegion(), source.getCredentials());
    StackOptionsFilesParser stackOptionsFilesParser = new StackOptionsFilesParser(objectMapper);
    Out out = new Out(stackOptionsFilesParser, cloudFormationClient, s3Client);
    VersionMetadata versionMetadata = out.run(workingDirectory, outInput.getSource(), outInput.getParams());
    objectMapper.writeValue(new BufferedOutputStream(System.out), versionMetadata);
  }
 

}

@ApplicationScoped
class CustomConfiguration {

  @Produces
  CommandLine customCommandLine(PicocliCommandLineFactory factory) {
    return factory.create().setExecutionExceptionHandler((e, commandLine, parseResult) -> {
      e.printStackTrace(); // no stack trace
      commandLine.getErr().println(e.getMessage());
      return commandLine.getCommandSpec().exitCodeOnExecutionException();
    });
  }
}