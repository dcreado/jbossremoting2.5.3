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

package org.jboss.test.remoting.lifecycle;

import junit.framework.TestCase;
import org.jboss.remoting.InvalidConfigurationException;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;

import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class InvokerLifecycleTestCase extends TestCase
{
   public InvokerLifecycleTestCase(String name)
   {
      super(name);
   }

   public void testMultipleConnectors() throws Exception
   {
      InvokerLocator serverLocator = new InvokerLocator("socket://localhost:2222");
      Connector connector1 = new Connector();
      connector1.setInvokerLocator(serverLocator.getLocatorURI());
      connector1.start();

      Connector connector2 = new Connector();
      connector2.setInvokerLocator(serverLocator.getLocatorURI());

      try
      {
         connector2.start();
      }
      catch(InvalidConfigurationException ice)
      {
         assertTrue("Got InvalidConfigurationException as expected.", true);
         return;
      }
      finally
      {
         connector1.stop();
         connector2.stop();
      }

      assertTrue("Did not get InvalidConfiguration which was NOT expected.", false);
   }

   public void testNonConcurrentConnectors() throws Exception
   {
      String defaultHost = InetAddress.getLocalHost().getHostName();
      String host = System.getProperty("jrunit.bind_addr", defaultHost);
      int port = PortUtil.findFreePort(host);
      InvokerLocator serverLocator = new InvokerLocator("socket://" + host + ":" + port);
      Connector connector1 = new Connector();
      connector1.setInvokerLocator(serverLocator.getLocatorURI());
      connector1.start();
      connector1.stop();

      Connector connector2 = new Connector();
      connector2.setInvokerLocator(serverLocator.getLocatorURI());

      try
      {
         connector2.start();
      }
      catch(InvalidConfigurationException ice)
      {
         assertTrue("Got InvalidConfigurationException which was unexpected.", false);
         return;
      }
      finally
      {
         connector2.stop();
      }

      assertTrue("Did not get InvalidConfiguration which is as expected.", true);

   }

   public void testStopConnector() throws Exception
   {
      // secure a server port to ensure is not available to connector
      InetAddress inetAddress = InetAddress.getByName("localhost");
      ServerSocket socket = new ServerSocket(3333, 0, inetAddress);

      String locatorURI = "socket://localhost:3333/?reuseAddress=false";
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      Connector connector = new Connector(serverLocator);
      try
      {
         connector.start();
         assertTrue("Should not have been able to start connector.  Should have gotten bind exception.", false);
      }
      catch (Exception e)
      {
         System.out.println("Got exception as expected.");
      }
      connector.stop();

      // have stopped, so free up port and restart
      socket.close();

      // should not be able to start
      connector.start();

      // startup must have been ok, now need to take down
      connector.stop();
      connector.destroy();

      // start the whole process over
      socket = new ServerSocket(3333, 0, inetAddress);
      connector = new Connector(serverLocator);
      try
      {
         connector.start();
         assertTrue("Should not have been able to start connector.  Should have gotten bind exception.", false);
      }
      catch (Exception e)
      {
         System.out.println("Got exception as expected.");
      }
      connector.stop();
      // this time, are also going to explicitly call destroy
      connector.destroy();

      // now close socket to release hold on port
      socket.close();
      Connector connector2 = new Connector(serverLocator);
      connector2.start();
      assertTrue("Able to start second connector.", true);


   }
}