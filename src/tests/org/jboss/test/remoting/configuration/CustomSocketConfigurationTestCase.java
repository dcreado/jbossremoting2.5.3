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
package org.jboss.test.remoting.configuration;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.RemoteClientInvoker;
import org.jboss.remoting.Remoting;

import javax.net.SocketFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class CustomSocketConfigurationTestCase extends TestCase
{

   public void testCustomSocketConfig() throws Exception
   {
      SocketFactory factory = SocketFactory.getDefault();
      System.out.println("Factory: " + factory);

      InvokerLocator locator = new InvokerLocator("sslsocket://localhost:9000");

      Map config = new HashMap();
      config.put(Remoting.CUSTOM_SOCKET_FACTORY, factory);

      Client client = new Client(locator, config);
      client.connect();

      RemoteClientInvoker invoker = (RemoteClientInvoker) client.getInvoker();
      SocketFactory invokerFactory = invoker.getSocketFactory();
      assertEquals(factory, invokerFactory);

   }
}