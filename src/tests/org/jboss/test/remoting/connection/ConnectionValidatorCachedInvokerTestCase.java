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
package org.jboss.test.remoting.connection;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;
import javax.net.ServerSocketFactory;

import org.apache.log4j.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.ConnectionListener;
import org.jboss.remoting.ConnectionValidator;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.InvokerRegistry;
import org.jboss.remoting.Remoting;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.util.TimerUtil;
import org.jboss.test.remoting.socketfactory.TestListener;
import org.jboss.test.remoting.socketfactory.CreationListenerTestRoot.TestHandler;

import junit.framework.TestCase;

/** 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1917 $
 * <p>
 * Copyright Jan 17, 2007
 * </p>
 */
public class ConnectionValidatorCachedInvokerTestCase
   extends TestCase
   implements ConnectionListener
{
   private static Logger log = Logger.getLogger(ConnectionValidatorCachedInvokerTestCase.class);

   private String locatorURI;
   private Connector connector;
   private Client client;

  
   public void setUp() throws Exception
   {
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      locatorURI = "socket://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      HashMap serverConfig = new HashMap();
      connector = new Connector(locator, serverConfig);
      connector.create();
      connector.addInvocationHandler("test", new TestHandler());
      connector.start();
      
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      client = new Client(locator, clientConfig);
      client.connect();
   }

   
   public void tearDown() throws Exception
   {
      if (connector != null)
      {
         connector.stop();
      }
      if(client != null)
      {
         client.disconnect();
      }
   }
   
   
   public void testTimerUtilDestroy() throws Throwable
   {
      log.info("entering " + getName());
      Integer i = (Integer) client.invoke(new Integer(7));
      assertEquals(8, i.intValue());
      client.addConnectionListener(this);
      Field field = Client.class.getDeclaredField("connectionValidator");
      field.setAccessible(true);
      ConnectionValidator cv = (ConnectionValidator) field.get(client);
      assertNotNull(cv);
      field = ConnectionValidator.class.getDeclaredField("stopped");
      field.setAccessible(true);
      boolean stopped = ((Boolean) field.get(cv)).booleanValue();
      assertFalse(stopped);
      
      TimerUtil.destroy();
      
      stopped = ((Boolean) field.get(cv)).booleanValue();
      assertTrue(stopped);
      log.info(getName() + " PASSES");
   }
   
   
   public void testConnectionValidatorStop() throws Throwable
   {
      log.info("entering " + getName());
      Integer i = (Integer) client.invoke(new Integer(7));
      assertEquals(8, i.intValue());
      client.addConnectionListener(this);
      Field field = Client.class.getDeclaredField("connectionValidator");
      field.setAccessible(true);
      ConnectionValidator cv = (ConnectionValidator) field.get(client);
      assertNotNull(cv);
      field = ConnectionValidator.class.getDeclaredField("stopped");
      field.setAccessible(true);
      boolean stopped = ((Boolean) field.get(cv)).booleanValue();
      assertFalse(stopped);
      
      cv.stop();
      
      stopped = ((Boolean) field.get(cv)).booleanValue();
      assertTrue(stopped);
      log.info(getName() + " PASSES");
   }
   
   
   public void testConnectionValidatorCancel() throws Throwable
   {
      log.info("entering " + getName());
      Integer i = (Integer) client.invoke(new Integer(7));
      assertEquals(8, i.intValue());
      client.addConnectionListener(this);
      Field field = Client.class.getDeclaredField("connectionValidator");
      field.setAccessible(true);
      ConnectionValidator cv = (ConnectionValidator) field.get(client);
      assertNotNull(cv);
      field = ConnectionValidator.class.getDeclaredField("stopped");
      field.setAccessible(true);
      boolean stopped = ((Boolean) field.get(cv)).booleanValue();
      assertFalse(stopped);
      
      cv.cancel();
      
      stopped = ((Boolean) field.get(cv)).booleanValue();
      assertTrue(stopped);
      log.info(getName() + " PASSES");
   }
   
   public void testDistinctClientInvoker() throws Throwable
   {
      client.addConnectionListener(this);
      Thread.sleep(3000);
      Field field = InvokerRegistry.class.getDeclaredField("clientLocators");
      field.setAccessible(true);
      Map clientLocators = (Map) field.get(null);
      List holderList = (List) clientLocators.get(new InvokerLocator(locatorURI));
      assertEquals(2, holderList.size());
   }


   public void handleConnectionException(Throwable throwable, Client client)
   {
      System.out.println("Got connection exception.");
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
