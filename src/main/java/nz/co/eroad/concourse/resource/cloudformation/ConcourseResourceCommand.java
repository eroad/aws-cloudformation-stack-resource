package nz.co.eroad.concourse.resource.cloudformation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.picocli.runtime.PicocliCommandLineFactory;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@picocli.CommandLine.Command(name = "concourse-resource")
public class ConcourseResourceCommand implements Runnable {


  @Spec
  private CommandSpec spec;


  private final ObjectMapper objectMapper;
  private final In in;
  private final Out out;
  private final Check check;



  
  @Inject
  public ConcourseResourceCommand(ObjectMapper objectMapper, In in, Out out, Check check) {
    this.objectMapper = objectMapper;
    this.in = in;
    this.out = out;
    this.check = check;
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
    List<?> versions = check.run(versionInput.getSource(), versionInput.getVersion());
    objectMapper.writeValue(new BufferedOutputStream(System.out), versions);
  }

  @CommandLine.Command(name = "in")
  private void in(@Parameters(arity = "1") String workingDirectory) throws IOException {
    VersionInput versionInput = objectMapper.readValue(
        System.in,
        VersionInput.class
    );
    if (versionInput == null) {
      throw new IllegalArgumentException("Input JSON was invalid!");
    }
    VersionMetadata versionMetadata = in.run(workingDirectory, versionInput.getSource(), versionInput.getVersion());
    objectMapper.writeValue(new BufferedOutputStream(System.out), versionMetadata);
  }

  @CommandLine.Command(name = "out")
  private void out(@Parameters(arity = "1") String workingDirectory) throws IOException {
    OutInput outInput = objectMapper.readValue(
        System.in,
        OutInput.class
    );
    if (outInput == null) {
      throw new IllegalArgumentException("Input JSON was invalid!");
    }
    VersionMetadata versionMetadata = out.run(workingDirectory, outInput.getSource(), outInput.getParams());
    objectMapper.writeValue(new BufferedOutputStream(System.out), versionMetadata);
  }
 

}

@ApplicationScoped
class CustomConfiguration {

  @Produces
  CommandLine customCommandLine(PicocliCommandLineFactory factory) {
    return factory.create().setExecutionExceptionHandler((e, commandLine, parseResult) -> {
      //ex.printStackTrace(); // no stack trace
      commandLine.getErr().println(e.getMessage());
      return commandLine.getCommandSpec().exitCodeOnExecutionException();
    });
  }
}