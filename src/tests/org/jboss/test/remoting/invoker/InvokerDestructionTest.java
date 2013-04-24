package org.jboss.test.remoting.invoker;


import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.logging.XLevel;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;


/**
 * Demonstrates the cost of recreating a client invoker with each invocation.
 * See JBREM-877.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Feb 23, 2008
 * </p>
 */
public class InvokerDestructionTest extends TestCase
{
   private static Logger log = Logger.getLogger(InvokerDestructionTest.class);
   
   private static boolean firstTime = true;
   protected static String metadata;
   
   protected String host;
   protected int port;
   protected String locatorURI;
   protected InvokerLocator serverLocator;
   protected Connector connector;
   protected TestInvocationHandler invocationHandler;

   
   public void setUp() throws Exception
   {
      if (firstTime)
      {
         firstTime = false;
         Logger.getLogger("org.jboss.remoting").setLevel(XLevel.INFO);
         Logger.getLogger("org.jboss.test.remoting").setLevel(Level.INFO);
         String pattern = "[%d{ABSOLUTE}] [%t] %5p (%F:%L) - %m%n";
         PatternLayout layout = new PatternLayout(pattern);
         ConsoleAppender consoleAppender = new ConsoleAppender(layout);
         Logger.getRootLogger().addAppender(consoleAppender);
         metadata = System.getProperty("remoting.metadata", "serializationtype=java");
      }
   }

   
   public void tearDown()
   {
   }
   

   
   public void testNoDelay() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(0);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      
      long l1 = doTest(clientLocator, clientConfig, 10000);
      log.info("delay 0: " + l1);
      
      shutdownServer();
      log.info(getName() + " PASSES");
   }

   
   public void testDelay() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(5000);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      
      long l1 = doTest(clientLocator, clientConfig, 10000);
      log.info("delay 5000: " + l1);
      
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   protected long doTest(InvokerLocator locator, Map config, int count) throws Throwable
   {
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++)
      {
         Client client = new Client(locator, config);
         client.connect();
         client.invoke("abc");
         client.disconnect();
      }
      return System.currentTimeMillis() - start;
   }
   
   protected String getTransport()
   {
      return "socket";
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   protected void setupServer(int delay) throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port;
      locatorURI += "/?" + Client.INVOKER_DESTRUCTION_DELAY + "=" + delay;
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("test", invocationHandler);
      connector.start();
   }
   
   
   protected void shutdownServer() throws Exception
   {
      if (connector != null)
         connector.stop();
   }
   
   
   static class TestInvocationHandler implements ServerInvocationHandler
   {
      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         return invocation.getParameter();
      }
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
}