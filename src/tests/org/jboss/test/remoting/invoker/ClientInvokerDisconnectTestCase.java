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

package org.jboss.test.remoting.invoker;

import java.net.InetAddress;

import junit.framework.TestCase;
import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.InvokerRegistry;
import org.jboss.remoting.RemoteClientInvoker;
import org.jboss.remoting.transport.ClientInvoker;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.local.LocalClientInvoker;

/**
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision: 3347 $</tt>
 *          <p/>
 *          $Id: ClientInvokerDisconnectTestCase.java 3347 2008-01-20 08:46:57Z ron.sigal@jboss.com $
 */
public class ClientInvokerDisconnectTestCase extends TestCase
{
   // Constants -----------------------------------------------------

   private static final Logger log = Logger.getLogger(ClientInvokerDisconnectTestCase.class);

   // Static --------------------------------------------------------

   // Attributes ----------------------------------------------------

   private String serverURI;
   private Connector server;

   // Constructors --------------------------------------------------

   public ClientInvokerDisconnectTestCase(String name)
   {
      super(name);
   }

   // Public --------------------------------------------------------

   public void setUp() throws Exception
   {
      super.setUp();

      String host = InetAddress.getLocalHost().getCanonicalHostName();
      int port = PortUtil.findFreePort(host);
      serverURI = "socket://" + host + ":" + port + "/";
      
      server = new Connector();
      server.setInvokerLocator(serverURI);
      server.start();
      log.info("Server " + serverURI + " started");
   }

   public void tearDown() throws Exception
   {
      server.stop();
      server = null;
      log.info("Server " + serverURI + " stopped");

      super.tearDown();
   }

   public void testLocalClientInvokerDisconnect() throws Throwable
   {
      Client client = new Client(new InvokerLocator(serverURI));
      client.connect();

      ClientInvoker[] clientInvokers = InvokerRegistry.getClientInvokers();
      assertEquals(1, clientInvokers.length);

      LocalClientInvoker clientInvoker = (LocalClientInvoker) clientInvokers[0];
      assertEquals(serverURI, clientInvoker.getLocator().getLocatorURI());


      client.disconnect();


      clientInvokers = InvokerRegistry.getClientInvokers();
      assertEquals(0, clientInvokers.length);
   }

   public void testRemoteClientInvokerDisconnect() throws Throwable
   {
      String passByValueServerURI = serverURI + "?byvalue=true";
      Client client = new Client(new InvokerLocator(passByValueServerURI));
      client.connect();

      ClientInvoker[] clientInvokers = InvokerRegistry.getClientInvokers();
      assertEquals(1, clientInvokers.length);

      RemoteClientInvoker clientInvoker = (RemoteClientInvoker) clientInvokers[0];
      assertEquals(passByValueServerURI, clientInvoker.getLocator().getLocatorURI());


      client.disconnect();


      clientInvokers = InvokerRegistry.getClientInvokers();
      assertEquals(0, clientInvokers.length);
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

}
