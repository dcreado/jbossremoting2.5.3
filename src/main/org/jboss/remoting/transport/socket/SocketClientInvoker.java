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

package org.jboss.remoting.transport.socket;

import org.jboss.logging.Logger;
import org.jboss.remoting.CannotConnectException;
import org.jboss.remoting.InvocationFailureException;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.serialization.ClassLoaderUtility;
import org.jboss.remoting.util.SecurityUtility;

import javax.net.SocketFactory;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.InetSocketAddress;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;

/**
 * SocketClientInvoker uses Sockets to remotely connect to the a remote ServerInvoker, which
 * must be a SocketServerInvoker.
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @version $Revision: 5739 $
 */
public class SocketClientInvoker extends MicroSocketClientInvoker
{
   private static final Logger log = Logger.getLogger(SocketClientInvoker.class);
   private static final boolean isTraceEnabled = log.isTraceEnabled();

   public static final String SO_TIMEOUT_FLAG = "timeout";

   /**
    * Default value for socket timeout is 30 minutes.
    */
   public static final int SO_TIMEOUT_DEFAULT = 1800000;

   protected int timeout = SO_TIMEOUT_DEFAULT;

   private Constructor clientSocketConstructor = null;

   /**
    * Set number of retries in getSocket method
    */
   public SocketClientInvoker(InvokerLocator locator)
   {
      this(locator, null);
   }

   public SocketClientInvoker(InvokerLocator locator, Map configuration)
   {
      super(locator, configuration);
      configureParameters();
   }

   protected ServerAddress createServerAddress(InetAddress addr, int port)
   {
      return new ServerAddress(addr.getHostAddress(), port, enableTcpNoDelay, timeout, maxPoolSize);
   }


   protected void configureParameters()
   {
      super.configureParameters();
      
      // For JBREM-1188
      timeout = SO_TIMEOUT_DEFAULT;
      
      Map params = configuration;
      if (params != null)
      {
         // look for socketTimeout param
         Object val = params.get(SO_TIMEOUT_FLAG);
         if (val != null)
         {
            try
            {
               timeout = Integer.valueOf((String) val).intValue();;
               log.debug(this + " setting timeout to " + timeout);
            }
            catch (Exception e)
            {
               log.warn(this + " could not convert " + SO_TIMEOUT_FLAG + " value of " +
                        val + " to a int value.");
            }
         }
      }
   }

   protected Object handleException(Exception ex, SocketWrapper socketWrapper)
         throws ClassNotFoundException, InvocationFailureException, CannotConnectException
   {
      if (ex instanceof ClassNotFoundException)
      {
         //TODO: -TME Add better exception handling for class not found exception
         log.debug("Error loading classes from remote call result.", ex);
         throw (ClassNotFoundException) ex;
      }
      
      if (ex instanceof CannotConnectException)
      {
         log.debug(this, ex);
         throw (CannotConnectException) ex;
      }
      
      if (ex instanceof SocketTimeoutException)
      {
         log.debug("Got SocketTimeoutException, exiting", ex);
         String message = "Socket timed out.  Waited " + socketWrapper.getTimeout() +
                          " milliseconds for response while calling on " + getLocator();
         throw new InvocationFailureException(message, ex);
      }

      if (ex instanceof InterruptedException)
      {
         log.debug(this, ex);
         throw new RuntimeException(ex);
      }
      
      throw new InvocationFailureException("Unable to perform invocation", ex);
   }

   /**
    * used for debugging (tracing) connections leaks
    */
   protected SocketWrapper createClientSocket(Socket socket, int timeout, Map metadata) throws Exception
   {
      if (clientSocketConstructor == null)
      {
         if(clientSocketClass == null)
         {
            clientSocketClass = ClassLoaderUtility.loadClass(getClass(), clientSocketClassName);
         }

         try
         {
            clientSocketConstructor = clientSocketClass.getConstructor(new Class[]{Socket.class, Map.class, Integer.class});
         }
         catch (NoSuchMethodException e)
         {
            clientSocketConstructor = clientSocketClass.getConstructor(new Class[]{Socket.class});
         }

      }

      SocketWrapper clientSocketWrapper = null;
      if (clientSocketConstructor.getParameterTypes().length == 3)
      {
         clientSocketWrapper = (SocketWrapper) clientSocketConstructor.newInstance(new Object[]{socket, metadata, new Integer(timeout)});
      }
      else
      {
         clientSocketWrapper = (SocketWrapper) clientSocketConstructor.newInstance(new Object[]{socket});
         clientSocketWrapper.setTimeout(timeout);
      }

      return clientSocketWrapper;
   }


   protected Socket createSocket(String address, int port, int timeout) throws IOException
   {
      Socket s = null;
      SocketFactory socketFactory = getSocketFactory();
      if (socketFactory != null)
      {
         s = socketFactory.createSocket();
      }
      else
      {
          s = new Socket();
      }

      configureSocket(s);
      InetSocketAddress inetAddr = new InetSocketAddress(address, port);
      
      if (timeout < 0)
      {
         timeout = getTimeout();
         if (timeout < 0)
            timeout = 0;
      }

      connect(s, inetAddr, timeout);
      return s;
   }

   protected SocketWrapper getPooledConnection()
   {
      SocketWrapper socketWrapper = null;
      while (pool.size() > 0)
      {
         socketWrapper = (SocketWrapper) pool.removeFirst();
         try
         {
            if (socketWrapper != null)
            {
               if (socketWrapper instanceof OpenConnectionChecker)
               {
                  ((OpenConnectionChecker) socketWrapper).checkOpenConnection();
               }
               if (shouldCheckConnection)
               {
                  socketWrapper.checkConnection();
                  return socketWrapper;
               }
               else
               {
                  if (socketWrapper.getSocket().isConnected())
                  {
                     return socketWrapper;
                  }
                  else
                  {
                     try
                     {
                        socketWrapper.close();
                     }
                     catch (IOException e)
                     {
                     }
                     return null;
                  }
               }
            }
         }
         catch (Exception ex)
         {
            if (isTraceEnabled)
            {
               log.trace("Couldn't reuse connection from pool", ex);
            }
            try
            {
               socketWrapper.close();
            }
            catch (Exception ignored)
            {
            }
         }
      }
      return null;
   }


   /**
    * Getter for property timeout
    *
    * @return Value of property timeout
    */
   public int getTimeout()
   {
      return timeout;
   }

   public String toString()
   {
      return "SocketClientInvoker[" + Integer.toHexString(System.identityHashCode(this)) + ", " +
         locator.getProtocol() + "://" + locator.getHost() + ":" + locator.getPort() + "]";
   }
   
   static private void connect(final Socket socket, final InetSocketAddress address, final int timeout)
   throws IOException
   {
      if (SecurityUtility.skipAccessControl())
      {
         socket.connect(address, timeout);
         return;
      }
      
      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               socket.connect(address, timeout);
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (IOException) e.getCause();
      }   
   }
}
