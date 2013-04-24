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

package org.jboss.remoting.transport.coyote.ssl;

import org.apache.tomcat.util.net.ServerSocketFactory;
import org.jboss.logging.Logger;
import org.jboss.remoting.security.ServerSocketFactoryMBean;
import org.jboss.remoting.security.ServerSocketFactoryWrapper;
import org.jboss.remoting.util.SecurityUtility;

import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class RemotingServerSocketFactory extends ServerSocketFactory
{
   private Map mbeanServerMap = null;
   private javax.net.ServerSocketFactory serverSocketFactory = null;

   private static Map serverSocketFactories = new HashMap();

   protected final Logger log = Logger.getLogger(getClass());

   private boolean performHandshake = false;

   public RemotingServerSocketFactory()
   {

   }

   public RemotingServerSocketFactory(Map mbeanServerMap)
   {
      //TODO: -TME - THIS IS A TOTAL HACK!!!
      // Am having to do this because need to get reference to
      // MBeanServer from original CoyoteInvoker so can lookup
      // the real ServerSocketFactory to use (which will be registered
      // within the mbean server).
      this.mbeanServerMap = mbeanServerMap;
   }

   private void init() throws InstantiationException
   {
      //TODO: -TME - check for nulls and fallback defaults (or throw exception)
      String locator = (String) attributes.get("locator");

      // first look to see if there is socket factory to use already set
      serverSocketFactory = (javax.net.ServerSocketFactory)serverSocketFactories.get(locator);
      if(serverSocketFactory == null)
      {


      MBeanServer mbeanserver = (MBeanServer) mbeanServerMap.get(locator);
      String serverSocketObjName = (String) attributes.get("serverSocketFactory");

      if(locator == null || mbeanserver == null || serverSocketObjName == null)
      {
         throw new InstantiationException("Can not create ServerSocketFactory with SSL support due " +
                                          "to one of the following being null." +
                                          "\nlocator = " + locator + "\nmbeanserver = " + mbeanserver +
                                          "\nserverSocketObjName = " + serverSocketObjName);
      }

      try
      {
         ServerSocketFactoryMBean serverSocketFactoryMBean = (ServerSocketFactoryMBean)
               MBeanServerInvocationHandler.newProxyInstance(mbeanserver,
                                                             new ObjectName(serverSocketObjName),
                                                             ServerSocketFactoryMBean.class,
                                                             false);
         serverSocketFactory = new ServerSocketFactoryWrapper(serverSocketFactoryMBean);
         performHandshake = true;
      }
      catch(Exception e)
      {
         InstantiationException iex = new InstantiationException("Error creating ServerSocketFactory proxy via MBeanServer");
         iex.setStackTrace(e.getStackTrace());
         throw iex;
      }
      }


   }

   /**
    * Returns a server socket which uses all network interfaces on
    * the host, and is bound to a the specified port.  The socket is
    * configured with the socket options (such as accept timeout)
    * given to this factory.
    *
    * @param port the port to listen to
    * @throws java.io.IOException    for networking errors
    * @throws InstantiationException for construction errors
    */
   public ServerSocket createSocket(int port) throws IOException, InstantiationException
   {
      if(serverSocketFactory == null)
      {
         init();
      }
      return serverSocketFactory.createServerSocket(port);
   }

   /**
    * Returns a server socket which uses all network interfaces on
    * the host, is bound to a the specified port, and uses the
    * specified connection backlog.  The socket is configured with
    * the socket options (such as accept timeout) given to this factory.
    *
    * @param port    the port to listen to
    * @param backlog how many connections are queued
    * @throws java.io.IOException    for networking errors
    * @throws InstantiationException for construction errors
    */

   public ServerSocket createSocket(int port, int backlog) throws IOException, InstantiationException
   {
      if(serverSocketFactory == null)
      {
         init();
      }
      return serverSocketFactory.createServerSocket(port, backlog);
   }

   /**
    * Returns a server socket which uses only the specified network
    * interface on the local host, is bound to a the specified port,
    * and uses the specified connection backlog.  The socket is configured
    * with the socket options (such as accept timeout) given to this factory.
    *
    * @param port      the port to listen to
    * @param backlog   how many connections are queued
    * @param ifAddress the network interface address to use
    * @throws java.io.IOException    for networking errors
    * @throws InstantiationException for construction errors
    */

   public ServerSocket createSocket(int port, int backlog, InetAddress ifAddress) throws IOException, InstantiationException
   {
      if(serverSocketFactory == null)
      {
         init();
      }

      ServerSocket svrSocket = serverSocketFactory.createServerSocket(port, backlog, ifAddress);
      return svrSocket;
   }

   /**
    * Wrapper function for accept(). This allows us to trap and
    * translate exceptions if necessary
    *
    * @throws java.io.IOException;
    */
   public Socket acceptSocket(ServerSocket serverSocket) throws IOException
   {
      return accept(serverSocket);
   }

   /**
    * Extra function to initiate the handshake. Sometimes necessary
    * for SSL
    *
    * @throws java.io.IOException;
    */
   public void handshake(Socket sock) throws IOException
   {
      if(performHandshake)
      {
         ((SSLSocket) sock).startHandshake();
      }
   }

   public static void setServerSocketFactory(String locatorURI, javax.net.ServerSocketFactory svrSocketFactory)
   {
      serverSocketFactories.put(locatorURI, svrSocketFactory);
   }
   
   static private Socket accept(final ServerSocket ss) throws IOException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return ss.accept();
      }
      
      try
      {
          return (Socket)AccessController.doPrivileged( new PrivilegedExceptionAction()
          {
             public Object run() throws Exception
             {
                 return ss.accept();
             }
          });
      }
      catch (PrivilegedActionException e)
      {
          throw (IOException) e.getCause();
      }
   }
}