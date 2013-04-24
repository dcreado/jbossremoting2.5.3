package org.jboss.test.remoting.lease;

import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.logging.XLevel;
import org.jboss.remoting.Client;
import org.jboss.remoting.ConnectionListener;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.w3c.dom.Document;


/**
 * 
 * Unit test for JBREM-1012.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Jul 18, 2008
 * </p>
 */
public class InjectedConnectionListenerTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(InjectedConnectionListenerTestCase.class);
   
   private static boolean firstTime = true;
   
   protected String host;
   protected int port;
   protected String locatorURI;
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
      }
      
      TestConnectionListener.gotException = false;
   }

   
   public void tearDown()
   {
   }
   
   
   public void testInjectionWithClassName() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      MBeanServer mbeanServer = MBeanServerFactory.createMBeanServer();
      setupServer(TestConnectionListener.class.getName(), mbeanServer);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Client.ENABLE_LEASE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Verify ConnectionListener is notified.
      log.info("client disconnecting");
      client.disconnect();
      log.info("client disconnected");
      assertTrue(TestConnectionListener.gotException);
      
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   
   public void testInjectionWithMBean() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      String connectionListenerName = "jboss:type=connectionlistener";
      ObjectName objName = new ObjectName(connectionListenerName);
      MBeanServer mbeanServer = MBeanServerFactory.createMBeanServer();
      TestConnectionListener listener = new TestConnectionListener();
      mbeanServer.registerMBean(listener, objName);
      setupServer(connectionListenerName, mbeanServer);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Client.ENABLE_LEASE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Verify ConnectionListener is notified.
      log.info("client disconnecting");
      client.disconnect();
      log.info("client disconnected");
      assertTrue(TestConnectionListener.gotException);
      
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   protected String getTransport()
   {
      return "socket";
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   protected void setupServer(String listener, MBeanServer mbeanServer) throws Exception
   {
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      Connector connector = new Connector(config);
      mbeanServer.registerMBean(connector, new ObjectName("test:type=connector"));
      
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      StringBuffer buf = new StringBuffer();
      buf.append("<?xml version=\"1.0\"?>\n");
      buf.append("<config>");
      buf.append("   <invoker transport=\"" + getTransport() + "\">");
      buf.append("      <attribute name=\"serverBindAddress\">" + host + "</attribute>");
      buf.append("      <attribute name=\"serverBindPort\">" + port + "</attribute>");
      buf.append("      <attribute name=\"" + ServerInvoker.CONNECTION_LISTENER + "\">" + listener + "</attribute>");
      buf.append("      <attribute name=\"" + ServerInvoker.CLIENT_LEASE_PERIOD + "\">5000</attribute>");
      buf.append("   </invoker>");
      buf.append("</config>");
      ByteArrayInputStream bais = new ByteArrayInputStream(buf.toString().getBytes());
      Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(bais);
      connector.setConfiguration(xml.getDocumentElement());
      connector.create();
      connector.addInvocationHandler("test", new TestInvocationHandler());
      connector.start();
      locatorURI = connector.getInvokerLocator();
      log.info("Started remoting server with locator uri of: " + locatorURI);
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
   
   
   public interface TestConnectionListenerMBean extends ConnectionListener
   {   
   }
   
   public static class TestConnectionListener implements TestConnectionListenerMBean
   {
      public static boolean gotException;
      
      public void handleConnectionException(Throwable throwable, Client client)
      {
         gotException = true;
         log.info("TestConnectionListener got exception");
      }
   }
}