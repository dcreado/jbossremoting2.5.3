package org.jboss.remoting.samples.chat.utility;

/**
 * <p>Title: Chat4</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */


public class Debug {
  private static boolean on = false;

  public static void turnOn()
  {
    on = true;
    System.out.println("Debug: chat debugging turned on");
  }

  public static void turnOff()
  {
    on = false;
    System.out.println("Debug: chat debugging turned on");
    }

  public static boolean isOn()
  {
    return on;
  }

  public static void println(String str) {
    if (on)
      if (str != null)
        System.out.println(str);
      else
        System.out.println("null");
  }
}
