
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
package org.jboss.test.remoting.transport.bisocket.multihome;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.remoting.Home;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transport.AddressUtil;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.bisocket.Bisocket;
import org.jboss.remoting.transport.bisocket.BisocketServerInvoker;
import org.jboss.test.remoting.multihome.MultihomeTestParent;
import org.jboss.test.remoting.multihome.TestInvocationHandler;


/**
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Jan 8, 2008
 * </p>
 */
public class BisocketMultihomeTestCase extends MultihomeTestParent
{
   protected ArrayList interfaces = new ArrayList();
   
   protected String getTransport()
   {
      return "bisocket";
   }
   
   protected void addExtraCallbackConfig(Map config)
   {
      config.put(Bisocket.IS_CALLBACK_SERVER, "true");
   }
   
   public void testSecondaryBindPorts() throws Throwable
   {
      log.info("entering " + getName());

      // Start server.
      localCreateLocatorURI();
      InetAddress address = (InetAddress) interfaces.get(0);
      int port = PortUtil.findFreePort(address.getHostAddress());
      Set secondaryBindPorts = new HashSet();
      secondaryBindPorts.add(new Integer(port));
      StringBuffer sb = new StringBuffer(Integer.toString(port));
      for (int i = 1; i < interfaces.size(); i++)
      {
         address = (InetAddress) interfaces.get(i);
         port = PortUtil.findFreePort(address.getHostAddress());
         sb.append('!').append(port);
         secondaryBindPorts.add(new Integer(port));
      }
      String secondaryBindPortsString = sb.toString();
      log.info("secondaryBindPorts: " + secondaryBindPortsString);
      locatorURI += "&" + Bisocket.SECONDARY_BIND_PORTS + "=" + secondaryBindPortsString;
      HashMap config = new HashMap();
      addExtraServerConfig(config);
      connector = new Connector(locatorURI, config);
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("test", invocationHandler);
      connector.start();
      
      // Check bind ports in secondary locator.
      BisocketServerInvoker invoker = (BisocketServerInvoker) connector.getServerInvoker();
      Field field = BisocketServerInvoker.class.getDeclaredField("secondaryLocator");
      field.setAccessible(true);
      InvokerLocator secondaryLocator = (InvokerLocator) field.get(invoker);
      log.info("secondaryLocator: " + secondaryLocator);
      List homes = secondaryLocator.getHomeList();
      Home h = (Home) homes.get(0);
      StringBuffer sb2 = new StringBuffer(Integer.toString(h.port));
      for (int i = 1; i < homes.size(); i++)
      {
         h = (Home) homes.get(i);
         sb2.append('!').append(h.port);
      }
      log.info("sb2: " + sb2.toString());
      assertEquals(secondaryBindPortsString, sb2.toString());
      
      // Check bind ports in secondary server sockets.
      field = BisocketServerInvoker.class.getDeclaredField("secondaryServerSockets");
      field.setAccessible(true);
      Set secondaryServerSockets = (Set) field.get(invoker);
      assertEquals(secondaryBindPorts.size(), secondaryServerSockets.size());
      Set ports = new HashSet();
      Iterator it = secondaryServerSockets.iterator();
      while (it.hasNext())
      {
         ServerSocket ss = (ServerSocket) it.next();
         ports.add(new Integer(ss.getLocalPort()));
      }
      assertEquals(secondaryBindPorts, ports);
      
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testSecondaryConnectPorts() throws Throwable
   {
      log.info("entering " + getName());

      // Start server.
      localCreateLocatorURI();
      InetAddress address = (InetAddress) interfaces.get(0);
      int port = PortUtil.findFreePort(address.getHostAddress());
      Set secondaryBindPorts = new HashSet();
      secondaryBindPorts.add(new Integer(port));
      StringBuffer sb = new StringBuffer(Integer.toString(port));
      for (int i = 1; i < interfaces.size(); i++)
      {
         address = (InetAddress) interfaces.get(i);
         port = PortUtil.findFreePort(address.getHostAddress());
         sb.append('!').append(port);
         secondaryBindPorts.add(new Integer(port));
      }
      String secondaryBindPortsString = sb.toString();
      log.info("secondaryBindPorts: " + secondaryBindPortsString);
      locatorURI += "&" + Bisocket.SECONDARY_BIND_PORTS + "=" + secondaryBindPortsString;
      
      address = (InetAddress) interfaces.get(0);
      port = PortUtil.findFreePort(address.getHostAddress());
      StringBuffer sb2 = new StringBuffer(Integer.toString(port));
      for (int i = 1; i < interfaces.size(); i++)
      {
         address = (InetAddress) interfaces.get(i);
         port = PortUtil.findFreePort(address.getHostAddress());
         sb2.append('!').append(port);
      }
      String secondaryConnectPortsString = sb2.toString();
      log.info("secondaryConnectPorts: " + secondaryConnectPortsString);
      locatorURI += "&" + Bisocket.SECONDARY_CONNECT_PORTS + "=" + secondaryConnectPortsString;
      log.info("locatorURI: " + locatorURI);
      HashMap config = new HashMap();
      addExtraServerConfig(config);
      connector = new Connector(locatorURI, config);
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("test", invocationHandler);
      connector.start();
      
      // Check connect ports in secondary locator.
      BisocketServerInvoker invoker = (BisocketServerInvoker) connector.getServerInvoker();
      Field field = BisocketServerInvoker.class.getDeclaredField("secondaryLocator");
      field.setAccessible(true);
      InvokerLocator secondaryLocator = (InvokerLocator) field.get(invoker);
      log.info("secondaryLocator: " + secondaryLocator);
      List homes = secondaryLocator.getHomeList();
      Home h = (Home) homes.get(0);
      StringBuffer sb3 = new StringBuffer(Integer.toString(h.port));
      for (int i = 1; i < homes.size(); i++)
      {
         h = (Home) homes.get(i);
         sb3.append('!').append(h.port);
      }
      log.info("sb3: " + sb3.toString());
      assertEquals(secondaryConnectPortsString, sb3.toString());
      
      // Check bind ports in secondary server sockets.
      field = BisocketServerInvoker.class.getDeclaredField("secondaryServerSockets");
      field.setAccessible(true);
      Set secondaryServerSockets = (Set) field.get(invoker);
      assertEquals(secondaryBindPorts.size(), secondaryServerSockets.size());
      Set ports = new HashSet();
      Iterator it = secondaryServerSockets.iterator();
      while (it.hasNext())
      {
         ServerSocket ss = (ServerSocket) it.next();
         ports.add(new Integer(ss.getLocalPort()));
      }
      assertEquals(secondaryBindPorts, ports);
      
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testMismatchedSecondaryPorts() throws Throwable
   {
      log.info("entering " + getName());

      // Start server.
      localCreateLocatorURI();
      InetAddress address = (InetAddress) interfaces.get(0);
      int port = PortUtil.findFreePort(address.getHostAddress());
      Set secondaryBindPorts = new HashSet();
      secondaryBindPorts.add(new Integer(port));
      StringBuffer sb = new StringBuffer(Integer.toString(port));
      for (int i = 1; i < interfaces.size(); i++)
      {
         address = (InetAddress) interfaces.get(i);
         port = PortUtil.findFreePort(address.getHostAddress());
         sb.append('!').append(port);
         secondaryBindPorts.add(new Integer(port));
      }
      String secondaryBindPortsString = sb.toString();
      log.info("secondaryBindPorts: " + secondaryBindPortsString);
      locatorURI += "&" + Bisocket.SECONDARY_BIND_PORTS + "=" + secondaryBindPortsString;
      
      address = (InetAddress) interfaces.get(0);
      port = PortUtil.findFreePort(address.getHostAddress());
      StringBuffer sb2 = new StringBuffer(Integer.toString(port));
      for (int i = 1; i < interfaces.size(); i++)
      {
         address = (InetAddress) interfaces.get(i);
         port = PortUtil.findFreePort(address.getHostAddress());
         sb2.append('!').append(port);
      }
      
      // Add extra secondary connect port.
      sb2.append('!').append(9876);
      String secondaryConnectPortsString = sb2.toString();
      log.info("secondaryConnectPorts: " + secondaryConnectPortsString);
      locatorURI += "&" + Bisocket.SECONDARY_CONNECT_PORTS + "=" + secondaryConnectPortsString;
      log.info("locatorURI: " + locatorURI);
      HashMap config = new HashMap();
      addExtraServerConfig(config);
      connector = new Connector(locatorURI, config);
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("test", invocationHandler);
      connector.start();
      
      // Check bind ports in secondary locator.
      BisocketServerInvoker invoker = (BisocketServerInvoker) connector.getServerInvoker();
      Field field = BisocketServerInvoker.class.getDeclaredField("secondaryLocator");
      field.setAccessible(true);
      InvokerLocator secondaryLocator = (InvokerLocator) field.get(invoker);
      log.info("secondaryLocator: " + secondaryLocator);
      List homes = secondaryLocator.getHomeList();
      Home h = (Home) homes.get(0);
      StringBuffer sb3 = new StringBuffer(Integer.toString(h.port));
      for (int i = 1; i < homes.size(); i++)
      {
         h = (Home) homes.get(i);
         sb3.append('!').append(h.port);
      }
      log.info("sb3: " + sb3.toString());
      assertEquals(secondaryBindPortsString, sb3.toString());
      
      // Check bind ports in secondary server sockets.
      field = BisocketServerInvoker.class.getDeclaredField("secondaryServerSockets");
      field.setAccessible(true);
      Set secondaryServerSockets = (Set) field.get(invoker);
      assertEquals(secondaryBindPorts.size(), secondaryServerSockets.size());
      Set ports = new HashSet();
      Iterator it = secondaryServerSockets.iterator();
      while (it.hasNext())
      {
         ServerSocket ss = (ServerSocket) it.next();
         ports.add(new Integer(ss.getLocalPort()));
      }
      assertEquals(secondaryBindPorts, ports);
      
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   protected void localCreateLocatorURI() throws Exception
   {
      StringBuffer sb = new StringBuffer();
      Enumeration e1 = NetworkInterface.getNetworkInterfaces();
      boolean first = true;
      int counter = 0;
      
      loop: while (e1.hasMoreElements())
      {
         NetworkInterface iface = (NetworkInterface) e1.nextElement();
         Enumeration e2 = iface.getInetAddresses();
         while (e2.hasMoreElements())
         {
            if (++counter > 5) break loop;
            InetAddress address = (InetAddress) e2.nextElement();
            String host = address.getHostAddress();
            
            if (AddressUtil.checkAddress(host))
            {
               log.info("host is functional: " + host);
               int port = PortUtil.findFreePort(host);
               if (first)
                  first = false;
               else
                  sb.append('!');
               sb.append(host).append(':').append(port);
               interfaces.add(address);
            }
            else
            {
               log.info("skipping host: " + host);
            }
         }
      }
      
      locatorURI = getTransport() + "://" + InvokerLocator.MULTIHOME + getPath() + "/?";
      locatorURI += InvokerLocator.HOMES_KEY + "=" + sb.toString();
   }
}

