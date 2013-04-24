/*
* JBoss, a division of Red Hat
* Copyright 2006, Red Hat Middleware, LLC, and individual contributors as indicated
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
package org.jboss.test.remoting.lease.servlet;

import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ConnectionListener;
import org.jboss.remoting.Client;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.ConnectorMBean;
import org.jboss.remoting.security.ServerSocketFactoryMBean;
import org.jboss.remoting.security.ServerSocketFactoryWrapper;
import org.jboss.remoting.callback.InvokerCallbackHandler;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import java.util.Iterator;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class ServletHandler implements ServerInvocationHandler, ConnectionListener
{

   public ServletHandler()
   {
      try
      {
         ObjectName connectorObjName = new ObjectName("jboss.remoting:service=Connector,transport=Servlet");
         MBeanServer mbeanServer = getMBeanServer();
         System.out.println("Found jboss mbean server.");
         if(mbeanServer != null)
         {
            ConnectorMBean servletConnector = (ConnectorMBean)MBeanServerInvocationHandler.newProxyInstance(mbeanServer, connectorObjName, ConnectorMBean.class, false);
            servletConnector.addConnectionListener(this);
         }
      }
      catch (MalformedObjectNameException e)
      {
         e.printStackTrace();
      }
   }

   protected MBeanServer getMBeanServer()
   {
      // the intention of having this as a separate protected method is for subclasses to override
      // it in case this servlet is not running in JBossAS and thus needs to find an non-JBoss
      // MBeanServer.  This design won't work however since when this servlet is loaded, it will
      // still need to load in this JBoss specific MBeanServerLocator.  But, this servlet also
      // requires JBoss logging too so its not like this is the only place that breaks if not running
      // in JBossAS.  To complete this design, we must make this parent servlet an abstract class,
      // which this method abstract.  Then we need to create a JBoss-specific subclass with this
      // method's code in its getMBeanServer().
      for (Iterator i = MBeanServerFactory.findMBeanServer(null).iterator(); i.hasNext();)
      {
         MBeanServer server = (MBeanServer) i.next();
         if (server.getDefaultDomain().equals("jboss"))
         {
            return server;
         }
      }
      return null;
   }


   public void setMBeanServer(MBeanServer server)
   {
   }

   public void setInvoker(ServerInvoker invoker)
   {
   }

   public Object invoke(InvocationRequest invocation) throws Throwable
   {
      return "foobar";
   }

   public void addListener(InvokerCallbackHandler callbackHandler)
   {
   }

   public void removeListener(InvokerCallbackHandler callbackHandler)
   {
   }

   public void handleConnectionException(Throwable throwable, Client client)
   {
      System.out.println("Connection exception: " + throwable.getMessage() + " for Client " + client);
   }
}