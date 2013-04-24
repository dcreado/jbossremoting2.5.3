package org.jboss.remoting.samples.chat.utility;

import java.util.*;
import java.io.*;
import javax.servlet.*;

/**
 * <p>Title: chat.utility.Parameters</p>
 * <p>Description: Manages parameters to the application.</p>
 * <p>A parameter's value is taken from:
 *   <ol>
       <li>the primary source, which is
           <ul>
               <li>the command line if the program is standalone
               <li>the <code>web.conf</code> file if the program is a servlet
           </ul>
          or, if the parameter is not given in the primary source,
       <li>a configuration file property with the same name, or, if there is no such configuration file property
       <li>the default value given in this file, or, if a default value is not given in this file,
       <li>null
     </ol>
 * </p>
 * <p>Copyright: Copyright (c) 2004</p>
 * @author Ron Sigal
 * @version 1.0
 */

public class Parameters
{
  static Hashtable parameterValues = new Hashtable();
  static Hashtable primaryParameterValues = new Hashtable();
  static Hashtable defaultParameterValues = new Hashtable();

  static
  {
      defaultParameterValues.put("serverUri", "http://localhost:8000"); // URI of chat server
      defaultParameterValues.put("clientUri", "http://localhost:8002"); // URI used by ChatReceiver's
  }


  /**
   * <code>initParameters()</code> puts parameters into a Hashtable.
   * <p>
   * For each element of <code>args</code> of the form "string<sub>1</sub>=string<sub>2</sub>",
   * the value "string<sub>2</sub>" is a parameter with key "string<sub>1</sub>".
   *
   * @param <code>args</code>: an array of Strings. These are meant to be the command line parameters.
   */
  public static void initParameters(String[] args)
  {
    // get command line parameters
    for (int i = 0; i < args.length; i++)
    {
      int mark = args[i].indexOf('=');
      if (mark >= 0)
      {
        primaryParameterValues.put(args[i].substring(0, mark), args[i].substring(mark + 1));
      }
    }

    getSecondaryParameterSources();
  }


  /**
   * <code>initParameters()</code> puts parameters into a Hashtable.
   * <p>
   *
   * @param <code>servletConfig/code>: the primary source of parameters
   */
  public static void initParameters(ServletConfig servletConfig)
  {
    Enumeration e = servletConfig.getInitParameterNames();

    while (e.hasMoreElements())
    {
      String key = (String) e.nextElement();
      primaryParameterValues.put(key, servletConfig.getInitParameter(key));
    }

    getSecondaryParameterSources();
  }


  /**
   * <code>getSecondaryParameterSources()</code>
   * <ul>
         <li>reads parameters from a parameter file named <code>chat.conf</code>, if it exists,
   *     <li>gets default parameter values, and
   *     <li>merges these with the parameter values from the primary source
   * </ul>
   */
  private static void getSecondaryParameterSources()
{
    // check if chat.conf parameter file has a non-standard location
    String configurationFilePath = "chat.conf";

    if (primaryParameterValues.containsKey("chat.conf"))
      configurationFilePath = (String) primaryParameterValues.get("chat.conf");

    File configurationFile = new File(configurationFilePath);

    // get properties from the configuration file
    if (configurationFile.exists())
    {
      try
      {
        FileInputStream fis = new FileInputStream(configurationFile);
        Properties properties = new Properties();
        properties.load(fis);
        Enumeration e = properties.propertyNames();

        while (e.hasMoreElements())
        {
          String key = (String) e.nextElement();
          parameterValues.put(key, properties.getProperty(key));
          System.out.println(key + ":" + properties.getProperty(key));
        }
      }
      catch (FileNotFoundException fnfe)
      {
        System.err.println("configuration file not found: " + configurationFilePath);
        System.err.println("using default properties");
      }
      catch (IOException ioe)
      {
        System.err.println("error reading configuration file: " + configurationFilePath);
        System.err.println("using default properties");
        System.out.println(ioe.getMessage());
      }

      String debug = getParameter("debug");

      if (debug.charAt(0) == 'y')
      {
        System.out.println("properties:");
        Iterator it = parameterValues.keySet().iterator();
        while (it.hasNext())
        {
          String key = (String) it.next();
          System.out.println("  " + key + ": " + parameterValues.get(key));
        }
      }
    }

    // override properties from configuration file with command line parameters
    Iterator it = primaryParameterValues.keySet().iterator();
    while (it.hasNext())
    {
      Object key = it.next();
      parameterValues.put(key, primaryParameterValues.get(key));
    }
  }



  /**
   * <code>getParameter()</code> returns stored parameter values
   *
   * @param <code>name</code> name of parameter whose value is requested
   * @return if <code>name</code> is a key in <code>parameters</code>, returns the value associated with <code>name</code>.
   *         Otherwise, returns <code>null</code>.
   */
  public static String getParameter(String name)
  {
     return getParameter(name, null);
  }
  
  
  /**
   * <code>getParameter()</code> returns stored parameter values
   *
   * @param <code>name</code> name of parameter whose value is requested
   * @return if <code>name</code> is a key in <code>parameters</code>, returns the value associated with <code>name</code>.
   *         Otherwise, returns <code>null</code>.
   */
  public static String getParameter(String name, String defaultValue)
  {
    String value = null;

    value = (String) parameterValues.get(name);

    if (value == null)
      value = System.getProperty(name);

    if (value == null)
      value = (String) defaultParameterValues.get(name);
    
    if (value == null)
       value = defaultValue;

    return value;
  }

  /**
   * <code>main()</code> implements unit tests.
   */
  public static void main(String[] args)
  {
    initParameters(args);
    System.out.println("clientUri: " + getParameter("clientUri"));
    System.out.println("serverUri: " + getParameter("serverUri"));

  }
}
