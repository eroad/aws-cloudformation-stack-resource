package nz.co.eroad.concourse.resource.cloudformation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.LogManager;
import nz.co.eroad.concourse.resource.cloudformation.check.Check;
import nz.co.eroad.concourse.resource.cloudformation.in.In;
import nz.co.eroad.concourse.resource.cloudformation.out.Out;
import nz.co.eroad.concourse.resource.cloudformation.pojo.OutInput;
import nz.co.eroad.concourse.resource.cloudformation.pojo.Source;
import nz.co.eroad.concourse.resource.cloudformation.pojo.Version;
import nz.co.eroad.concourse.resource.cloudformation.pojo.VersionInput;
import nz.co.eroad.concourse.resource.cloudformation.pojo.VersionMetadata;
import nz.co.eroad.concourse.resource.cloudformation.util.Colorizer;
import picocli.CommandLine;
import picocli.CommandLine.Help.Ansi.Style;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@picocli.CommandLine.Command(name = "concourse-resource")
public class ConcourseResourceCommand implements Runnable {


  @Spec
  private CommandSpec spec;


  private final ObjectMapper objectMapper = new ObjectMapper()
      .registerModule(new JavaTimeModule());

  public ConcourseResourceCommand() {

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

    Check check = new Check(source);
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

    In in = new In(source, objectMapper);
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
    StackOptionsFilesParser stackOptionsFilesParser = new StackOptionsFilesParser(objectMapper);
    Out out = new Out(stackOptionsFilesParser, source);
    VersionMetadata versionMetadata = out.run(workingDirectory, outInput.getParams());
    objectMapper.writeValue(new BufferedOutputStream(System.out), versionMetadata);
  }
 

    public static void main(String... args) { // bootstrap the application

      LogManager.getLogManager().reset();
      System.exit(new CommandLine(new ConcourseResourceCommand())
          .setExecutionExceptionHandler(
              (e, commandLine, parseResult) -> {
                //ex.printStackTrace(); // no stack trace
                commandLine.getErr().println(Colorizer.colorize(e.getMessage(), Style.fg_red));
                return commandLine.getCommandSpec().exitCodeOnExecutionException();
              }).execute(args));
    }

}