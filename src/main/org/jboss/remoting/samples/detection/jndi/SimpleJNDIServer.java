package org.jboss.remoting.samples.detection.jndi;

import org.jboss.remoting.util.SecurityUtility;
import org.jnp.server.Main;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * A JNDI server that should be run before running the simple detector client/server.
 * Leave running while shutting down different server instances.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class SimpleJNDIServer
{
   // Default locator values - command line args can override transport and port
   private static String transport = "socket";
   private static String host = "localhost";
   private static int port = 5400;
   private int detectorPort = 1099;


   /**
    * Can pass transport and port to be used as parameters. Valid transports are 'rmi' and 'socket'.
    *
    * @param args transport and port number
    */
   public static void main(String[] args)
   {
      // get system property -Dargs that is in format "transport:port"
      String prop = System.getProperty("args");
      if(prop != null)
      {
         try
         {
            SimpleJNDIServer.transport = prop.substring(0, prop.indexOf("-"));
            SimpleJNDIServer.port = Integer.parseInt(prop.substring(prop.indexOf("-") + 1));
         }
         catch(NumberFormatException nfe)
         {
            SimpleJNDIServer.println("INVALID ARGUMENTS: Bad port from property args: " + prop);
            System.exit(1);
         }
         catch(Exception e)
         {
            SimpleJNDIServer.println("INVALID ARGUMENTS: -Dargs property must be in the form '{socket|rmi}-{port#}': " + prop);
            System.exit(1);
         }
      }

      // command line args override defaults and system property
      if((args != null) && (args.length != 0))
      {
         if(args.length == 2)
         {
            SimpleJNDIServer.transport = args[0];
            SimpleJNDIServer.port = Integer.parseInt(args[1]);
         }
         else
         {
            SimpleJNDIServer.println("INVALID ARGUMENTS: Usage: " + SimpleJNDIServer.class.getName()
                                         + " [rmi|socket <port>]");
            System.exit(1);
         }
      }

      SimpleJNDIServer.println("Starting JNDI server... to stop this server, kill it manually via Control-C");

      SimpleJNDIServer server = new SimpleJNDIServer();
      try
      {
         server.setupJNDIServer();

         // wait forever, let the user kill us at any point (at which point, the client will detect we went down)
         while(true)
         {
            Thread.sleep(1000);
         }
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }

      SimpleJNDIServer.println("Stopping JBoss/Remoting server");
   }

   private void setupJNDIServer() throws Exception
   {
      Object namingBean = null;
      Class namingBeanImplClass = null;
      try
      {
         namingBeanImplClass = Class.forName("org.jnp.server.NamingBeanImpl");
         namingBean = namingBeanImplClass.newInstance();
         Method startMethod = namingBeanImplClass.getMethod("start", new Class[] {});
         setSystemProperty("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
         startMethod.invoke(namingBean, new Object[] {});
      }
      catch (Exception e)
      {
         SimpleJNDIServer.println("Cannot find NamingBeanImpl: must be running jdk 1.4");
      }
      
      // start JNDI server
      String detectorHost = InetAddress.getLocalHost().getHostName();

      Main JNDIServer = new Main();
      if (namingBean != null)
      {
         Class namingBeanClass = Class.forName("org.jnp.server.NamingBean");
         Method setNamingInfoMethod = JNDIServer.getClass().getMethod("setNamingInfo", new Class[] {namingBeanClass});
         setNamingInfoMethod.invoke(JNDIServer, new Object[] {namingBean});
      }
      JNDIServer.setPort(detectorPort);
      JNDIServer.setBindAddress(detectorHost);
      JNDIServer.start();
      System.out.println("Started JNDI server on " + detectorHost + ":" + detectorPort);

   }

   /**
    * Outputs a message to stdout.
    *
    * @param msg the message to output
    */
   public static void println(String msg)
   {
      System.out.println(new java.util.Date() + ": [SERVER]: " + msg);
   }
   
   static private void setSystemProperty(final String name, final String value)
   {
      if (SecurityUtility.skipAccessControl())
      {
         System.setProperty(name, value);
         return;
      }
      
      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return System.setProperty(name, value);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RuntimeException) e.getCause();
      }
   }
}
