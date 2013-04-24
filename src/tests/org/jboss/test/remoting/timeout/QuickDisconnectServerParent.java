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
package org.jboss.test.remoting.timeout;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.jrunit.extensions.ServerTestCase;
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


/** 
 * QuickDisconnectTestParent verifies that LeasePinger can set its
 * own short timeout value when it is called during Client.disconnect(), so that,
 * even if the server is unavailable, Client.disconnect() can finish quickly.
 * 
 * It also tests that Client can set its own short timeout value when in
 * removeListener().
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2922 $
 * <p>
 * Copyright Jan 24, 2007
 * </p>
 */
public abstract class QuickDisconnectServerParent extends ServerTestCase
{
   public static final String START_SERVER = "startServer";
   public static final int port = 3999;
   
   protected static Logger log = Logger.getLogger(QuickDisconnectServerParent.class);
   protected static boolean firstTime = true;
   
   protected Connector connector;
   protected boolean receivedConnectionException;
   
   public void setUp() throws Exception
   {
      String host = InetAddress.getLocalHost().getHostAddress();
      String locatorURI = getTransport() + "://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      HashMap serverConfig = new HashMap();
      addServerConfig(serverConfig);
      connector = new Connector(locator, serverConfig);
      connector.create();
      connector.addInvocationHandler("test", new TestHandler());
      connector.addConnectionListener(new TestListener());
      connector.start();
   }
   
   
   public void tearDown()
   {
      connector.stop();
   }
   
   
   protected abstract String getTransport();

   
   protected void addServerConfig(Map config)
   {  
   }
   
   
   public class TestHandler implements ServerInvocationHandler
   {
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         if (START_SERVER.equals(invocation.getParameter()))
         {
            int port = PortUtil.findFreePort(InetAddress.getLocalHost().getHostName());
            new ServerStarter(port).start();
            return new Integer(port);
         }
         else
         {
            return invocation.getParameter();
         }
      }

      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
   }
   
   
   public class TestListener implements ConnectionListener
   {
      public void handleConnectionException(Throwable throwable, Client client)
      {
         log.info("received connection exception");
      }
   }
   
   
   public class ServerStarter extends Thread
   {
      int port;
      
      public ServerStarter(int port)
      {
         this.port = port;
      }
      
      public void run()
      {
         try
         {
            String host = InetAddress.getLocalHost().getHostAddress();
            String locatorURI = getTransport() + "://" + host + ":" + port;
            InvokerLocator locator = new InvokerLocator(locatorURI);
            HashMap serverConfig = new HashMap();
            addServerConfig(serverConfig);
            final Connector connector = new Connector(locator, serverConfig);
            connector.create();
            connector.addInvocationHandler("test", new TestHandler());
            connector.addConnectionListener(new TestListener());
            connector.start();
            log.info("Connector started: " + connector.getInvokerLocator());
            Thread.sleep(10000);
            connector.stop();
            log.info("Connector stopped: " + connector.getInvokerLocator());
         }
         catch (Exception e)
         {
            
         }
      }
   }
}
