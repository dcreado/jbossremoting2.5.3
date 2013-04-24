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

/*
 * Created on Jan 3, 2006
 */
package org.jboss.test.remoting.configuration;

import java.net.InetAddress;

import junit.framework.TestCase;

import org.jboss.logging.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.InvokerRegistry;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.transport.Connector;


/**
 * A ServerBindTest.

 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * @version $Revision: 593 $
 * <p>
 * Copyright (c) 2005
 * </p>
 */

public class ServerBindingTestCase extends TestCase
{
   protected static final Logger log = Logger.getLogger(ServerBindingTestCase.class);
   private Connector connector;
   
   public void setUp() throws Exception
   {
      connector = new Connector();
   }
   

   public void tearDown() throws Exception
   {
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   
   public void testBindAddress()
   {
      String uri = "socket://192.0.0.1:8081/?serverBindAddress=localhost";
      assertTrue(doOneBindingTest(uri, "localhost", 8081));
   }
   
   
   public void testConnectAddress()
   {
      try
      {
         String uri = "socket://0.1.2.3:8082/?clientConnectAddress=1.2.3.4";
         assertTrue(doOneBindingTest(uri, InetAddress.getLocalHost().getHostAddress(), 8082));
      }
      catch (Exception e)
      {
         e.printStackTrace();
         log.error(e);
         fail();
      }
   }
   
   
   public void testBindAndConnectAddress()
   {
      try
      {
         String uri = "socket://0.1.2.3:8083/?serverBindAddress=localhost&clientConnectAddress=1.2.3.4";
         assertTrue(doOneBindingTest(uri, "localhost", 8083));
      }
      catch (Exception e)
      {
         e.printStackTrace();
         log.error(e);
         fail();
      }
   }
   
   
   public void testLocatorAddress()
   {
      try
      {
         String uri = "socket://localhost:8084";
         assertTrue(doOneBindingTest(uri, "127.0.0.1", 8084));
      }
      catch (Exception e)
      {
         e.printStackTrace();
         log.error(e);
         fail();
      }
   }
   
   
   public void testNullLocatorAddress()
   {
      try
      {
         String uri = "socket://:8085";
         assertTrue(doOneBindingTest(uri, "127.0.0.1", 8085));
      }
      catch (Exception e)
      {
         e.printStackTrace();
         log.error(e);
         fail();
      }
   }
   
   
   public void testBindPort()
   {
      String uri = "socket://localhost:1111/?serverBindPort=8086";
      assertTrue(doOneBindingTest(uri, "127.0.0.1", 8086));
   }

   
   public void testZeroBindPort()
   {
      String uri = "socket://localhost/?serverBindPort=0";
      assertTrue(doOneAnonymousBindingTest(uri));
   }

   
   public void testNegativeBindPort()
   {
      String uri = "socket://localhost/?serverBindPort=-1";
      assertTrue(doOneAnonymousBindingTest(uri));
   }
   
   
   public void testZeroConnectPort()
   {
      String uri = "socket://localhost/?clientConnectPort=0";
      assertTrue(doOneAnonymousBindingTest(uri));
   }

   
   public void testNegativeConnectPort()
   {
      String uri = "socket://localhost/?serverBindPort=-1";
      assertTrue(doOneAnonymousBindingTest(uri));
   }
   
   public void testZeroBindandConnectPort()
   {
      String uri = "socket://localhost/?serverBindPort=0&clientConnectPort=0";
      assertTrue(doOneAnonymousBindingTest(uri));
   }

   
   public void testNegativeBindandConnectPort()
   {
      String uri = "socket://localhost/?serverBindPort=-1&clientConnectPort=-1";
      assertTrue(doOneAnonymousBindingTest(uri));
   }  
   
   
   public void testZeroLocatorPort()
   {
      String uri = "socket://localhost:0";
      assertTrue(doOneAnonymousBindingTest(uri));
   }

   
   public void testNegativeLocatorPort()
   {
      String uri = "socket://localhost:-1";
      assertTrue(doOneAnonymousBindingTest(uri));
   }
   
   
   protected boolean doOneBindingTest(String uri, String expectedHost, int expectedPort)
   {
      boolean success = true;
      
      try
      {
         connector.setInvokerLocator(uri);
         connector.create();
         connector.start();
         
         ServerInvoker[] serverInvokers = InvokerRegistry.getServerInvokers();
         
         if (serverInvokers == null || serverInvokers.length == 0)
         {
            log.error("no invoker created: " + uri);
            success = false;
         }
         
         ServerInvoker serverInvoker = serverInvokers[0];
         String bindHost = serverInvoker.getServerBindAddress();
         int bindPort = serverInvoker.getServerBindPort();
         
         if (!expectedHost.equals(bindHost))
         {
            log.error("host (" + bindHost + ") != expected host (" + expectedHost);
            success = false;
         }
         
         if (expectedPort != bindPort)
         {
            log.error("port (" + bindPort + ") != expected port (" + expectedPort);
            success = false;
         }

         return success;
      }
      catch (Exception e)
      {
         e.printStackTrace();
         log.error(e);
         return false;
      }
   }

   
   protected boolean doOneAnonymousBindingTest(String uri)
   {
      boolean success = true;
      
      try
      {
         connector.setInvokerLocator(uri);
         connector.create();
         connector.start();
         InvokerLocator locator = new InvokerLocator(connector.getInvokerLocator());
         
         if (locator.getPort() <= 0)
         {
            log.error("port should be > 0: " + locator.getPort());
            success = false;
         }

         return success;
      }
      catch (Exception e)
      {
         e.printStackTrace();
         log.error(e);
         return false;
      }
   }
}

