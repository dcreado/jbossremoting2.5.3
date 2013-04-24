/*
* JBoss, Home of Professional Open Source
* Copyright 2005, JBoss Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/

package org.jboss.test.remoting.transport.http.ssl.custom;

import java.io.ByteArrayInputStream;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.xml.parsers.DocumentBuilderFactory;
import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.security.SSLServerSocketFactoryService;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.security.ServerSocketFactoryMBean;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.coyote.ssl.RemotingSSLImplementation;
import org.jboss.test.remoting.transport.http.ssl.SSLInvokerConstants;
import org.jboss.test.remoting.transport.web.ComplexObject;
import org.w3c.dom.Document;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class HTTPSInvokerTestServer extends ServerTestCase implements SSLInvokerConstants
{
   // String to be returned from invocation handler upon client invocation calls.
   public static final String RESPONSE_VALUE = "This is the return to SampleInvocationHandler invocation";
   public static final ComplexObject OBJECT_RESPONSE_VALUE = new ComplexObject(5, "dub", false);
   public static final ComplexObject LARGE_OBJECT_RESPONSE_VALUE = new ComplexObject(5, "dub", false, 7568);

   public static final String NULL_RETURN_PARAM = "return_null";
   public static final String OBJECT_RETURN_PARAM = "return_object";

   private Connector connector = null;

   public void setupServer() throws Exception
   {
      /*
      String locatorURI = transport + "://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      System.out.println("Starting remoting server with locator uri of: " + locatorURI);
      connector = new Connector();
      connector.setInvokerLocator(locator.getLocatorURI());
      connector.create();

      ServerSocketFactory svrSocketFactory = createServerSocketFactory();
      HTTPSServerInvoker httpsSvrInvoker = (HTTPSServerInvoker) connector.getServerInvoker();
      httpsSvrInvoker.setServerSocketFactory(svrSocketFactory);

      SampleInvocationHandler invocationHandler = new SampleInvocationHandler();
      // first parameter is sub-system name.  can be any String value.
      connector.addInvocationHandler("sample", invocationHandler);

      connector.start();
      */

      MBeanServer server = MBeanServerFactory.createMBeanServer();

      // create and register socket server factory service
      ServerSocketFactoryMBean serverSocketFactoryMBean = createServerSocketFactoryMBean();
      String socketFactoryObjName = "test:type=serversocketfactory";
      server.registerMBean(serverSocketFactoryMBean, new ObjectName(socketFactoryObjName));

      connector = new Connector();
      server.registerMBean(connector, new ObjectName("test:type=connector,transport=coyote"));
      StringBuffer buf = new StringBuffer();
      buf.append("<?xml version=\"1.0\"?>\n");
      buf.append("<config>");
      buf.append("<invoker transport=\"" + transport + "\">");
      buf.append("<attribute name=\"SSLImplementation\">" + RemotingSSLImplementation.class.getName() + "</attribute>");
      buf.append("<attribute name=\"serverSocketFactory\">" + socketFactoryObjName + "</attribute>");
      buf.append("<attribute name=\"serverBindAddress\">" + host + "</attribute>");
      buf.append("<attribute name=\"serverBindPort\">" + port + "</attribute>");
      buf.append("</invoker>");
      buf.append("<handlers>");
      buf.append("  <handler subsystem=\"sample\">" + HTTPSInvokerTestServer.SampleInvocationHandler.class.getName() + "</handler>\n");
      buf.append("</handlers>");
      buf.append("</config>");
      Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(buf.toString().getBytes()));
      //connector.setInvokerLocator(locator.getLocatorURI());
      connector.setConfiguration(xml.getDocumentElement());
      connector.create();
      connector.start();


   }

   private ServerSocketFactoryMBean createServerSocketFactoryMBean() throws Exception
   {
      ServerSocketFactoryMBean serverSocketFactoryMBean = null;

      SSLSocketBuilder socketBuilder = new SSLSocketBuilder();
      socketBuilder.setUseSSLServerSocketFactory(false);

      socketBuilder.setSecureSocketProtocol("SSL");
      socketBuilder.setKeyStoreAlgorithm("SunX509");

      socketBuilder.setKeyStoreType("JKS");
      String keyStoreFilePath = this.getClass().getResource("../.keystore").getFile();
      socketBuilder.setKeyStoreURL(keyStoreFilePath);
      socketBuilder.setKeyStorePassword("unit-tests-server");
      /*
       * This is optional since if not set, will use
       * the key store password (and are the same in this case)
       */
      //server.setKeyPassword("unit-tests-server");

      SSLServerSocketFactoryService sslServerSocketFactoryService = new SSLServerSocketFactoryService();
      sslServerSocketFactoryService.setSSLSocketBuilder(socketBuilder);
      sslServerSocketFactoryService.start();
      serverSocketFactoryMBean = sslServerSocketFactoryService;

      return serverSocketFactoryMBean;
   }

/*
   private ServerSocketFactory createServerSocketFactory()
         throws NoSuchAlgorithmException, KeyManagementException, IOException,
                CertificateException, UnrecoverableKeyException, KeyStoreException
   {

      ServerSocketFactory serverSocketFactory = null;

      SSLSocketBuilder server = new SSLSocketBuilder();
      server.setUseSSLServerSocketFactory(false);

      server.setSecureSocketProtocol("SSL");
      server.setKeyManagementAlgorithm("SunX509");

      server.setKeyStoreType("JKS");
      String keyStoreFilePath = this.getClass().getResource("../.keystore").getFile();
      server.setKeyStoreURL(keyStoreFilePath);
      server.setKeyStorePassword("unit-tests-server");

      // This is optional since if not set, will use
      // the key store password (and are the same in this case)
      //server.setKeyPassword("unit-tests-server");

      serverSocketFactory = server.createSSLServerSocketFactory();

      return serverSocketFactory;
   }
*/

   protected void setUp() throws Exception
   {
      setupServer();
   }

   protected void tearDown() throws Exception
   {
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   public static void main(String[] args)
   {
      HTTPSInvokerTestServer server = new HTTPSInvokerTestServer();
      try
      {
         server.setUp();
         Thread.currentThread().sleep(600000);
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }

   }

   /**
    * Simple invocation handler implementation.
    */
   public static class SampleInvocationHandler implements ServerInvocationHandler
   {


      /**
       * called to handle a specific invocation
       *
       * @param invocation
       * @return
       * @throws Throwable
       */
      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         // Print out the invocation request
         System.out.println("Invocation request is: " + invocation.getParameter());
         if(NULL_RETURN_PARAM.equals(invocation.getParameter()))
         {
            return null;
         }
         else if(invocation.getParameter() instanceof ComplexObject)
         {
            ComplexObject obj = (ComplexObject) invocation.getParameter();
            if(obj.getSize() > 1024)
            {
               return LARGE_OBJECT_RESPONSE_VALUE;
            }
            else
            {
               return OBJECT_RESPONSE_VALUE;
            }
         }
         else
         {
            // Just going to return static string as this is just simple example code.
            return RESPONSE_VALUE;
         }
      }

      /**
       * Adds a callback handler that will listen for callbacks from
       * the server invoker handler.
       *
       * @param callbackHandler
       */
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         // NO OP as do not handling callback listeners in this example
      }

      /**
       * Removes the callback handler that was listening for callbacks
       * from the server invoker handler.
       *
       * @param callbackHandler
       */
      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         // NO OP as do not handling callback listeners in this example
      }

      /**
       * set the mbean server that the handler can reference
       *
       * @param server
       */
      public void setMBeanServer(MBeanServer server)
      {
         // NO OP as do not need reference to MBeanServer for this handler
      }

      /**
       * set the invoker that owns this handler
       *
       * @param invoker
       */
      public void setInvoker(ServerInvoker invoker)
      {
         // NO OP as do not need reference back to the server invoker
      }

   }

}