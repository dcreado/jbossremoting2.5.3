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

package org.jboss.remoting.transport.rmi;

import org.jboss.logging.Logger;
import org.jboss.remoting.util.SecurityUtility;

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.rmi.server.RMIServerSocketFactory;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;


/**
 * <code>RemotingRMIServerSocketFactory</code> provides two services to <code>RMIServerInvoker</code>.
 * <ol>
 * <li>It can be parameterized by a host name, allowing <code>RMIServerInvoker</code> to supply RMI
 * with a factory which creates server sockets bound to a specified host name as well as port.
 * <p/>
 * <li>It can be parameterized by a <code>ServerSocketFactory</code> allowing <code>RMIServerInvoker</code>
 * to supply RMI with a factory facility which creates specialized server sockets.
 * </ol>
 * <p/>
 * If the <code>ServerSocketFactory</code> parameter is specified, then the <code>RemotingRMIServerSocketFactory</code>
 * should be used with a matching instance of <code>RemotingRMIClientSocketFactory</code> with a compatible
 * <code>SocketFactory</code>.
 * <p/>
 * If the <code>ServerSocketFactory</code> parameter is not specified, an instance of <code>java.net.ServerSocket</code>
 * will be created by default.
 * <p/>
 *
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * @version $Revision: 5017 $
 *          <p/>
 *          Copyright (c) 2005
 *          </p>
 */

public class RemotingRMIServerSocketFactory implements RMIServerSocketFactory, Serializable
{
   protected static final Logger log = Logger.getLogger(RemotingRMIServerSocketFactory.class);

   private ServerSocketFactory serverSocketFactory;
   private int backlog;
   private InetAddress bindAddress;
   private int timeout;
   private static final long serialVersionUID = -5851426317709480542L;

   // The commented code below is from an attempt to incorporate a <code>java.lang.reflect.Constructor</code>
   // parameter to provide a very general way to create server sockets.  The problem is that
   // <code>Constructor</code> does not implement <code>Serializable</code>, which is necessary to
   // allow the <code>RemotingRMIServerSocketFactory</code> to be transmitted to the RMI Registry.  The
   // code is left in place because it could be resurrected by passing in a class name and parameter
   // types to specify a constructor.  Fortunately, <code>java.lang.Class</code> does implement
   // <code>Serializable</code>.

//   private Constructor constructor;
//   private Object[] args;
//   private int portPosition;


   /**
    * @param bindHost name of host to which all generated server sockets should be bound
    */
   public RemotingRMIServerSocketFactory(String bindHost) throws UnknownHostException
   {
      this(null, -1, bindHost);
   }


   /**
    * @param backlog  to be passed to all generated server sockets
    * @param bindHost name of host to which all generated server sockets should be bound
    * @throws UnknownHostException if an IP address for <code>bindHost</code> cannot be found
    */
   public RemotingRMIServerSocketFactory(int backlog, String bindHost) throws UnknownHostException
   {
      this(null, backlog, bindHost);
   }


   /**
    * @param serverSocketFactory <code>ServerSocketFactory</code> for generating server sockets
    * @param backlog             to be passed to all generated server sockets
    * @param bindHost            name of host to which all generated server sockets should be bound
    * @throws UnknownHostException if an IP address for <code>bindHost</code> cannot be found
    */
   public RemotingRMIServerSocketFactory(ServerSocketFactory serverSocketFactory, int backlog, String bindHost)
         throws UnknownHostException
   {
      this(serverSocketFactory, backlog, bindHost, 60000); // TODO: -TME This needs to be fixed so only comes from parent class
   }

   public RemotingRMIServerSocketFactory(ServerSocketFactory serverSocketFactory, int backlog, final String bindHost, int timeout)
         throws UnknownHostException
   {
      this.serverSocketFactory = serverSocketFactory;
      this.backlog = backlog;
      this.timeout = timeout;
      this.bindAddress = getAddressByName(bindHost);
   }

   public RemotingRMIServerSocketFactory(String bindHost, int timeout) throws UnknownHostException
   {
      this(null, -1, bindHost, timeout);
   }

//   public RemotingRMIServerSocketFactory(Constructor constructor, Object[] args, int portPosition)
//   {
//      this.constructor = constructor;
//      this.args = args;
//      this.portPosition = portPosition;
//   }


   /**
    * Creates a server socket bound to the host name passed to the constructor.  If a
    * <code>ServerSocketFactory</code> was passed to the constructor, it will be used to create the
    * server socket.  Otherwise, an instance of <code>java.net.ServerSocket</code> will be created.
    *
    * @param port the port to which the generated server socket should be bound
    * @return a new <code>ServerSocket</code>
    * @throws IOException if there is a problem creating a server socket
    */
   public ServerSocket createServerSocket(final int port) throws IOException
   {
      ServerSocket svrSocket = null;

      if(serverSocketFactory != null)
      {
         svrSocket = createServerSocket(serverSocketFactory, port, backlog, bindAddress);
      }

//      if (constructor != null)
//      {
//         try
//         {
//            if (portPosition != -1)
//               args[portPosition] = new Integer(port);
//
//            return (ServerSocket) constructor.newInstance(args);
//         }
//         catch (Exception e)
//         {
//            throw new IOException(e.getMessage());
//         }
//      }

      else
      {
         svrSocket = createServerSocket(port, backlog, bindAddress);
      }

      svrSocket.setSoTimeout(timeout);
      return svrSocket;
   }


   /**
    * Overrides the <code>equals()</code> method provided by the <code>Object</code> class.  It looks for
    * equality of binding host name and server socket factory parameters passed to constructor.
    *
    * @param o <code>Object</code> to which <code>code</code> is to be compared.
    * @return true if and only if <code>o</code> equals <code>this</code>
    */
   public boolean equals(Object o)
   {
      if(! (o instanceof RemotingRMIServerSocketFactory))
      {
         return false;
      }

      RemotingRMIServerSocketFactory ssf = (RemotingRMIServerSocketFactory) o;

      // This is for the version that uses a ServerSocketFactory
      if(serverSocketFactory != null)
      {
         if(ssf.serverSocketFactory == null ||
            !serverSocketFactory.equals(ssf.serverSocketFactory) ||
            backlog != ssf.backlog ||
            !bindAddress.equals(ssf.bindAddress))
         {
            return false;
         }

         return true;
      }

//      // This is for the version that uses a constructor
//      if (constructor != null)
//      {
//         if (ssf.constructor == null ||
//               ! constructor.equals(ssf.constructor) ||
//               portPosition != ssf.portPosition)
//            return false;
//
//         for (int i = 0; i < args.length; i++)
//         {
//            if (! args[i].equals(ssf.args[i]))
//               return false;
//         }
//
//         return true;
//      }

      // This is for the plain vanilla version
      if(ssf.serverSocketFactory != null || backlog != ssf.backlog || !bindAddress.equals(ssf.bindAddress))
      {
         return false;
      }

      return true;
   }


   /**
    * Overrides <code>hashCode()</code> method provided by the <code>Object</code> class.
    *
    * @return a hashcode for <code>this</code>
    */
   public int hashCode()
   {
      if(serverSocketFactory != null)
      {
         return serverSocketFactory.hashCode() * backlog * bindAddress.hashCode();
      }

//      if (constructor != null)
//      {
//         int hash = portPosition;
//         
//         for (int i = 0; i < args.length; i++)
//            hash *= args[i].hashCode();
//         
//         return hash;
//      }

      return backlog * bindAddress.hashCode();
   }

   static private ServerSocket createServerSocket(final ServerSocketFactory ssf,
                                                 final int port, final int backlog,
                                                 final InetAddress inetAddress)
   throws IOException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return ssf.createServerSocket(port, backlog, inetAddress);
      }

      try
      {
         return (ServerSocket)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return ssf.createServerSocket(port, backlog, inetAddress);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (IOException) e.getCause();
      }
   }

   static private ServerSocket createServerSocket(final int port, final int backlog,
                                                 final InetAddress inetAddress)
   throws IOException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return new ServerSocket(port, backlog, inetAddress);
      }

      try
      {
         return (ServerSocket)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IOException
            {
               return new ServerSocket(port, backlog, inetAddress);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (IOException) e.getCause();
      }
   }
   
   static private InetAddress getAddressByName(final String host) throws UnknownHostException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return InetAddress.getByName(host);
      }
      
      try
      {
         return (InetAddress)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IOException
            {
               return InetAddress.getByName(host);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (UnknownHostException) e.getCause();
      }
   }
}