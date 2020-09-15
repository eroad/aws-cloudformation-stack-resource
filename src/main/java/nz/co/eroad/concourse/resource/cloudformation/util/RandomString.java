package nz.co.eroad.concourse.resource.cloudformation.util;

import java.util.concurrent.ThreadLocalRandom;

public class RandomString {
  private static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

  public static String randomString( int len ){
    ThreadLocalRandom random = ThreadLocalRandom.current();
    StringBuilder sb = new StringBuilder( len );
    for( int i = 0; i < len; i++ )
      sb.append( AB.charAt(random.nextInt(AB.length()) ) );
    return sb.toString();
  }

}
