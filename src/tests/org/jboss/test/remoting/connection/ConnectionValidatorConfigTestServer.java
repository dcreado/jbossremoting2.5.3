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

import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;

import org.apache.log4j.Logger;
import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.ConnectionListener;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;


/**
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2473 $
 * <p>
 * Copyright Jun 15, 2007
 * </p>
 */
public class ConnectionValidatorConfigTestServer extends ServerTestCase
{
   public static String serverLocatorURI = "socket://localhost:7887";
   
   private static Logger log = Logger.getLogger(ConnectionValidatorConfigTestServer.class);
   
   // remoting server connector
   private Connector connector;

   
   /**
    * Sets up target remoting server.
    */
   public void setUp() throws Exception
   {
      // Start server.
      InvokerLocator serverLocator = new InvokerLocator(serverLocatorURI);
      log.info("Starting remoting server with locator uri of: " + serverLocator);
      HashMap config = new HashMap();
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      ServerInvocationHandler invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
   }

   
   public void tearDown()
   {
      if (connector != null)
         connector.stop();
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   
   
   public static void main(String[] args)
   {
      try
      {
         ConnectionValidatorConfigTestServer server = new ConnectionValidatorConfigTestServer();
         server.setUp();
         Thread.sleep(60000);
         server.shutdown();
      }
      catch (Throwable t)
      {
         t.printStackTrace();
      }
   }
   

   static class TestInvocationHandler implements ServerInvocationHandler
   {
      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public Object invoke(final InvocationRequest invocation) {return invocation.getParameter();}
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }

   static class TestConnectionListener implements ConnectionListener
   {
      public void handleConnectionException(Throwable throwable, Client client) {}
   }
}