package org.jboss.test.remoting.transport.socket.ssl.builder;

import org.apache.log4j.Level;
import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.logging.Logger;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.security.SSLServerSocketFactoryService;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.security.ServerSocketFactoryMBean;
import org.jboss.remoting.transport.Connector;
import org.jboss.test.remoting.TestUtil;
import org.jboss.test.remoting.transport.mock.MockServerInvocationHandler;
import org.jboss.test.remoting.transport.socket.ssl.SSLInvokerConstants;
import org.w3c.dom.Document;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * This is the server for the test case to verify can have push callbacks using ssl where
 * the client mode is used for the client calling back on the callback server living within the
 * client test instance.
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class SSLSocketInvokerTestServer extends ServerTestCase implements SSLInvokerConstants
{
   private int serverPort;
   private Connector connector = null;
   private SSLSocketBuilder socketBuilder = null;

   private static final Logger log = Logger.getLogger(SSLSocketInvokerTestServer.class);

   public void init() throws Exception
   {
      serverPort = getPort();
      if (serverPort < 0)
      {
         serverPort = TestUtil.getRandomPort();
      }
      log.debug("port = " + serverPort);


      MBeanServer server = MBeanServerFactory.createMBeanServer();

      // create and register socket server factory service
      createSSLSocketBuilder();

      ServerSocketFactoryMBean serverSocketFactoryMBean = createServerSocketFactoryMBean();
      String serverSocketFactoryObjName = "test:type=serversocketfactory";
      server.registerMBean(serverSocketFactoryMBean, new ObjectName(serverSocketFactoryObjName));

      connector = new Connector();
      server.registerMBean(connector, new ObjectName("test:type=connector,transport=sslsocket"));
      StringBuffer buf = new StringBuffer();
      buf.append("<?xml version=\"1.0\"?>\n");
      buf.append("<config>");
      buf.append("<invoker transport=\"" + getTransport() + "\">");
      buf.append("<attribute name=\"serverSocketFactory\">" + serverSocketFactoryObjName + "</attribute>");
      buf.append("<attribute name=\"serverBindAddress\">" + host + "</attribute>");
      buf.append("<attribute name=\"serverBindPort\">" + serverPort + "</attribute>");
      buf.append("<attribute name=\"leasePeriod\">" + 20000 + "</attribute>");
      buf.append("<attribute name=\"timeout\">" + 0 + "</attribute>");
      buf.append("<attribute name=\"socket.check_connection\" isParam=\"true\">" + 0 + "</attribute>");
      buf.append("</invoker>");
      buf.append("<handlers>");
      buf.append("  <handler subsystem=\"" + getSubsystem() + "\">" + getServerInvocationHandler().getClass().getName() + "</handler>\n");
      buf.append("</handlers>");
      buf.append("</config>");
      Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(buf.toString().getBytes()));
      connector.setConfiguration(xml.getDocumentElement());
      connector.create();
      connector.start();
   }

   private ServerSocketFactoryMBean createServerSocketFactoryMBean() throws Exception
   {
      ServerSocketFactoryMBean serverSocketFactoryMBean = null;
      SSLServerSocketFactoryService sslServerSocketFactoryService = new SSLServerSocketFactoryService();
      sslServerSocketFactoryService.setSSLSocketBuilder(socketBuilder);
      sslServerSocketFactoryService.start();
      serverSocketFactoryMBean = sslServerSocketFactoryService;

      return serverSocketFactoryMBean;
   }

   private void createSSLSocketBuilder() throws IOException
   {
      socketBuilder = new SSLSocketBuilder();
      socketBuilder.setUseSSLServerSocketFactory(false);

//      socketBuilder.setSecureSocketProtocol("SSL");
      socketBuilder.setSecureSocketProtocol("TLS");
      socketBuilder.setKeyStoreAlgorithm("SunX509");

      socketBuilder.setKeyStoreType("JKS");
      String keyStoreFilePath = this.getClass().getResource("../.keystore").getFile();
//      String keyStoreFilePath = this.getClass().getResource("messaging.keystore").getFile();
      socketBuilder.setKeyStoreURL(keyStoreFilePath);
      socketBuilder.setKeyStorePassword("unit-tests-server");
//      socketBuilder.setKeyStorePassword("secureexample");
      /*
       * This is optional since if not set, will use
       * the key store password (and are the same in this case)
       */
//      socketBuilder.setKeyPassword("secureexample");
      socketBuilder.setKeyPassword("unit-tests-server");

   }


   protected String getTransport()
   {
      return transport;
   }
   
   protected int getPort()
   {
      return port;
   }

   protected String getSubsystem()
   {
      return "mock";
   }

   protected ServerInvocationHandler getServerInvocationHandler()
   {
      return new MockServerInvocationHandler();
   }

   protected void setUp() throws Exception
   {
      org.apache.log4j.BasicConfigurator.configure();
      org.apache.log4j.Category.getRoot().setLevel(Level.INFO);
      org.apache.log4j.Category.getInstance("org.jgroups").setLevel(Level.INFO);
      org.apache.log4j.Category.getInstance("org.jboss.remoting").setLevel(Level.DEBUG);
      org.apache.log4j.Category.getInstance("test").setLevel(Level.DEBUG);

      init();
   }

   protected void tearDown() throws Exception
   {
      if (connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   public static void main(String[] args)
   {
      SSLSocketInvokerTestServer server = new SSLSocketInvokerTestServer();
      try
      {
         server.setUp();

         Thread.sleep(600000);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

}
