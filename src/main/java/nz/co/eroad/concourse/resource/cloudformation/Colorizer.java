package nz.co.eroad.concourse.resource.cloudformation;

import picocli.CommandLine.Help.Ansi.Style;

public class Colorizer {

  public static String colorize(String toColorise, Style ... styles) {
    return Style.on(styles) + toColorise + Style.reset.on();
  }
}
