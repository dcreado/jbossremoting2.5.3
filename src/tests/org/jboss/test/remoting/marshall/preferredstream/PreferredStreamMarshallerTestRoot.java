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
package org.jboss.test.remoting.marshall.preferredstream;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.socket.ClientSocketWrapper;
import org.jboss.remoting.transport.socket.LRUPool;
import org.jboss.remoting.transport.socket.MicroSocketClientInvoker;
import org.jboss.remoting.transport.socket.ServerSocketWrapper;
import org.jboss.remoting.transport.socket.ServerThread;
import org.jboss.remoting.transport.socket.SocketServerInvoker;

/** 
 * This test verifies that the socket transport caches and reuses object streams
 * provided by PreferredStream(Un)Marshallers.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2255 $
 * <p>
 * Copyright Jan 10, 2007
 * </p>
 */
public abstract class PreferredStreamMarshallerTestRoot extends TestCase
{
   protected static Logger log = Logger.getLogger(PreferredStreamMarshallerTestRoot.class);
   protected static boolean firstTime = true;
   
   
   public void setUp() throws Exception
   {
      if (firstTime)
      {
         firstTime = false;
         Logger.getLogger("org.jboss.remoting").setLevel(Level.INFO);
         Logger.getLogger("org.jboss.test.remoting").setLevel(Level.INFO);
         String pattern = "[%d{ABSOLUTE}] [%t] %5p (%F:%L) - %m%n";
         PatternLayout layout = new PatternLayout(pattern);
         ConsoleAppender consoleAppender = new ConsoleAppender(layout);
         Logger.getRootLogger().addAppender(consoleAppender);  
      }
   }
   
   
   
   public void testSerializableMarshallerUnMarshaller() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port;
      locatorURI += "/?marshaller=org.jboss.remoting.marshal.serializable.SerializableMarshaller";
      locatorURI += "&unmarshaller=org.jboss.remoting.marshal.serializable.SerializableUnMarshaller";
      InvokerLocator locator = new InvokerLocator(locatorURI);
      Connector connector = new Connector(locator);
      connector.create();
      connector.addInvocationHandler("test", new TestHandler());
      connector.start();
      
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      Client client = new Client(locator, config);
      client.connect();
      Integer i = (Integer) client.invoke(new Integer(17));
      assertEquals(18, i.intValue());
      
      // Make sure client has object streams.
      assertTrue(client.getInvoker() instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker clientInvoker = (MicroSocketClientInvoker) client.getInvoker();
      Field field = MicroSocketClientInvoker.class.getDeclaredField("pool");
      field.setAccessible(true);
      List pool = (List) field.get(clientInvoker);
      assertEquals(1, pool.size());
      ClientSocketWrapper csw = (ClientSocketWrapper) pool.get(0);
      field = ClientSocketWrapper.class.getDeclaredField("in");
      field.setAccessible(true);
      InputStream client_in1 = (InputStream) field.get(csw);
      assertTrue(client_in1 instanceof ObjectInputStream);
      field = ClientSocketWrapper.class.getDeclaredField("out");
      field.setAccessible(true);
      OutputStream client_out1 = (OutputStream) field.get(csw);
      assertTrue(client_out1 instanceof ObjectOutputStream);
      
      // Make sure server has object streams.
      assertTrue(connector.getServerInvoker() instanceof SocketServerInvoker);
      SocketServerInvoker ssi = (SocketServerInvoker) connector.getServerInvoker();
      field = SocketServerInvoker.class.getDeclaredField("clientpool");
      field.setAccessible(true);
      LRUPool clientpool = (LRUPool) field.get(ssi);
      assertEquals(1, clientpool.size());
      Set threads = clientpool.getContents();
      ServerThread serverThread = (ServerThread) threads.iterator().next();
      field = ServerThread.class.getDeclaredField("socketWrapper");
      field.setAccessible(true);
      ServerSocketWrapper ssw = (ServerSocketWrapper) field.get(serverThread);
      field = ClientSocketWrapper.class.getDeclaredField("in");
      field.setAccessible(true);
      InputStream server_in1 = (InputStream) field.get(ssw);
      assertTrue(server_in1 instanceof ObjectInputStream);
      field = ClientSocketWrapper.class.getDeclaredField("out");
      field.setAccessible(true);
      OutputStream server_out1 = (OutputStream) field.get(ssw);
      assertTrue(server_out1 instanceof ObjectOutputStream);
      
      // Do another invocation.
      i = (Integer) client.invoke(new Integer(19));
      assertEquals(20, i.intValue());
      
      // Make sure client and server reused the cached streams.
      field = ClientSocketWrapper.class.getDeclaredField("in");
      field.setAccessible(true);
      InputStream client_in2 = (InputStream) field.get(csw);
      assertTrue(client_in2 instanceof ObjectInputStream);
      field = ClientSocketWrapper.class.getDeclaredField("out");
      field.setAccessible(true);
      OutputStream client_out2 = (OutputStream) field.get(csw);
      assertTrue(client_out2 instanceof ObjectOutputStream);
      field = ClientSocketWrapper.class.getDeclaredField("in");
      field.setAccessible(true);
      InputStream server_in2 = (InputStream) field.get(ssw);
      assertTrue(server_in2 instanceof ObjectInputStream);
      field = ClientSocketWrapper.class.getDeclaredField("out");
      field.setAccessible(true);
      OutputStream server_out2 = (OutputStream) field.get(ssw);
      assertTrue(server_out2 instanceof ObjectOutputStream);
      assertEquals(client_in1, client_in2);
      assertEquals(client_out1, client_out2);
      assertEquals(server_in1, server_in2);
      assertEquals(server_out1, server_out2);      
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   public void testTestMarshallerUnMarshaller() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port;
      locatorURI += "/?marshaller=org.jboss.test.remoting.marshall.preferredstream.TestMarshaller";
      locatorURI += "&unmarshaller=org.jboss.test.remoting.marshall.preferredstream.TestUnMarshaller";
      InvokerLocator locator = new InvokerLocator(locatorURI);
      Connector connector = new Connector(locator);
      connector.create();
      connector.addInvocationHandler("test", new TestHandler());
      connector.start();
      
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      Client client = new Client(locator, config);
      client.connect();
      Integer i = (Integer) client.invoke(new Integer(17));
      assertEquals(18, i.intValue());
      
      // Make sure client has object streams from test directory.
      assertTrue(client.getInvoker() instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker clientInvoker = (MicroSocketClientInvoker) client.getInvoker();
      Field field = MicroSocketClientInvoker.class.getDeclaredField("pool");
      field.setAccessible(true);
      List pool = (List) field.get(clientInvoker);
      assertEquals(1, pool.size());
      ClientSocketWrapper csw = (ClientSocketWrapper) pool.get(0);
      field = ClientSocketWrapper.class.getDeclaredField("in");
      field.setAccessible(true);
      InputStream client_in1 = (InputStream) field.get(csw);
      assertTrue(client_in1 instanceof TestObjectInputStream);
      field = ClientSocketWrapper.class.getDeclaredField("out");
      field.setAccessible(true);
      OutputStream client_out1 = (OutputStream) field.get(csw);
      assertTrue(client_out1 instanceof TestObjectOutputStream);
      
      // Make sure server has object streams from test directory.
      assertTrue(connector.getServerInvoker() instanceof SocketServerInvoker);
      SocketServerInvoker ssi = (SocketServerInvoker) connector.getServerInvoker();
      field = SocketServerInvoker.class.getDeclaredField("clientpool");
      field.setAccessible(true);
      LRUPool clientpool = (LRUPool) field.get(ssi);
      assertEquals(1, clientpool.size());
      Set threads = clientpool.getContents();
      ServerThread serverThread = (ServerThread) threads.iterator().next();
      field = ServerThread.class.getDeclaredField("socketWrapper");
      field.setAccessible(true);
      ServerSocketWrapper ssw = (ServerSocketWrapper) field.get(serverThread);
      field = ClientSocketWrapper.class.getDeclaredField("in");
      field.setAccessible(true);
      InputStream server_in1 = (InputStream) field.get(ssw);
      assertTrue(server_in1 instanceof TestObjectInputStream);
      field = ClientSocketWrapper.class.getDeclaredField("out");
      field.setAccessible(true);
      OutputStream server_out1 = (OutputStream) field.get(ssw);
      assertTrue(server_out1 instanceof TestObjectOutputStream);
      
      // Do another invocation.
      i = (Integer) client.invoke(new Integer(19));
      assertEquals(20, i.intValue());
      
      // Make sure client and server reused the cached streams.
      field = ClientSocketWrapper.class.getDeclaredField("in");
      field.setAccessible(true);
      InputStream client_in2 = (InputStream) field.get(csw);
      assertTrue(client_in2 instanceof ObjectInputStream);
      field = ClientSocketWrapper.class.getDeclaredField("out");
      field.setAccessible(true);
      OutputStream client_out2 = (OutputStream) field.get(csw);
      assertTrue(client_out2 instanceof ObjectOutputStream);
      field = ClientSocketWrapper.class.getDeclaredField("in");
      field.setAccessible(true);
      InputStream server_in2 = (InputStream) field.get(ssw);
      assertTrue(server_in2 instanceof TestObjectInputStream);
      field = ClientSocketWrapper.class.getDeclaredField("out");
      field.setAccessible(true);
      OutputStream server_out2 = (OutputStream) field.get(ssw);
      assertTrue(server_out2 instanceof TestObjectOutputStream);
      assertEquals(client_in1, client_in2);
      assertEquals(client_out1, client_out2);
      assertEquals(server_in1, server_in2);
      assertEquals(server_out1, server_out2);      
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   protected abstract String getTransport();
   

   protected void addExtraClientConfig(Map config)
   {  
   }
   
   
   protected void addExtraServerConfig(Map config)
   {
   }
   
   
   public class TestHandler implements ServerInvocationHandler
   {

      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         Integer i = (Integer) invocation.getParameter();
         return new Integer(i.intValue() + 1);
      }

      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
   }
}