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
package org.jboss.test.remoting.client;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

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

/**
 * ClientSerializationTestCase verifies that org.jboss.remoting.Client can be
 * serialized.
 * 
 * See JBREM-708: http://jira.jboss.org/jira/browse/JBREM-708
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2225 $
 * <p>
 * Copyright Feb 23, 2007
 * </p>
 */
public class ClientSerializationTestCase extends TestCase
{
   protected static Logger log = Logger.getLogger(ClientSerializationTestCase.class);
   protected static boolean firstTime = true;
   
   protected static final String TEST = "test";
   protected static final String RETURN_CLIENT = "returnClient";
   
   protected InvokerLocator locator;
   protected Connector connector;
   protected Client client;
   
   
   public void setUp() throws Exception
   {
      if (firstTime)
      {
         firstTime = false;
         Logger.getLogger("org.jboss.remoting").setLevel(Level.DEBUG);
         Logger.getLogger("org.jboss.test.remoting").setLevel(Level.DEBUG);
         String pattern = "[%d{ABSOLUTE}] [%t] %5p (%F:%L) - %m%n";
         PatternLayout layout = new PatternLayout(pattern);
         ConsoleAppender consoleAppender = new ConsoleAppender(layout);
         Logger.getRootLogger().addAppender(consoleAppender);  
      }
   }

   
   public void tearDown() throws Exception
   {
      client.disconnect();
      connector.stop();
   }
   
   
   /**
    * Calling client.invoke(RETURN_CLIENT) will cause the invoker handler to return
    * a Client.  This method uses java serialization.
    */
   public void testJavaSerialization() throws Throwable
   {
      log.info("entering " + getName());
      init("java");
      assertEquals(TEST, client.invoke(TEST));
      Object response = client.invoke(RETURN_CLIENT);
      assertTrue(response instanceof Client);
      log.info("received Client");
      Client returnedClient = (Client) response;
      assertEquals(TEST, returnedClient.invoke(TEST));
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * Calling client.invoke(RETURN_CLIENT) will cause the invoker handler to return
    * a Client.  This method uses jboss serialization.
    */
   public void testJBossSerialization() throws Throwable
   {
      log.info("entering " + getName());
      init("jboss");
      assertEquals(TEST, client.invoke(TEST));
      Object response = client.invoke(RETURN_CLIENT);
      assertTrue(response instanceof Client);
      log.info("received Client");
      Client returnedClient = (Client) response;
      assertEquals(TEST, returnedClient.invoke(TEST));
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * Create a Connector and a Client.
    */
   protected void init(String serializationType) throws Exception
   {
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port;
      locatorURI += "/?serializationtype=" + serializationType;
      log.info("server locator: " + locatorURI);
      locator = new InvokerLocator(locatorURI);
      HashMap serverConfig = new HashMap();
      addServerConfig(serverConfig);
      connector = new Connector(locator, serverConfig);
      connector.create();
      connector.addInvocationHandler("test", new TestHandler());
      connector.start();
      
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addClientConfig(clientConfig);
      client = new Client(locator, clientConfig);
      client.connect();
   }
   
   
   protected String getTransport()
   {
      return "socket";
   }
   
   
   protected void addServerConfig(Map config)
   {  
   }
   
   
   protected void addClientConfig(Map config)
   {  
   }
   
   
   public class TestHandler implements ServerInvocationHandler
   {

      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         String command = (String) invocation.getParameter();
         if (TEST.equals(command))
         {
            return command;
         }
         else if (RETURN_CLIENT.equals(command))
         {
            HashMap config = new HashMap();
            config.put(InvokerLocator.FORCE_REMOTE, "true");
            Client client = new Client(locator, config);
            client.connect();
            
            if (TEST.equals(client.invoke(TEST)))
               return client;
            else
               throw new Exception("unable to create working client");
         }
         else
         {
            log.error("unrecognized command: " + command);
            throw new Exception("unrecognized command: " + command);
         }
      }

      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
   }
}
