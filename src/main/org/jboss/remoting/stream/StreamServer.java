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

package org.jboss.remoting.stream;

import org.jboss.logging.Logger;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.util.SecurityUtility;

import javax.management.MBeanServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * This is a helper class that runs internal to remoting on the
 * client side.  It contains a reference to a local input stream
 * and creates a remoting server to receive calls from a target
 * remoting server (via calls from a StreamHandler initiated by a
 * server invoker handler).
 * <p/>
 * NOTE: That once this class receives the close() method called
 * from the server, it will also stop and destroy the internal
 * remoting server, since is assumed there will be no more callbacks
 * (since the stream itself is closed).
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class StreamServer
{
   private InputStream streamSource = null;

   private String transport = "socket";
   private String host = "localhost";
   private int port = 5405;

   private Connector connector = null;

   private boolean internalConnector = true;

   private static final Logger log = Logger.getLogger(StreamServer.class);

   public static final String STREAM_TRANSPORT_KEY = "remoting.stream.transport";
   public static final String STREAM_HOST_KEY = "remoting.stream.host";
   public static final String STREAM_PORT_KEY = "remoting.stream.port";


   /**
    * Creates the server wrapped around the specified input stream.
    * This will create the remoting server as well.
    *
    * @param stream
    * @throws Exception
    */
   public StreamServer(InputStream stream) throws Exception
   {
      this.streamSource = stream;
      String locatorURI = getLocatorURI();
      setupServer(locatorURI);
   }

   public StreamServer(InputStream stream, InvokerLocator locator) throws Exception
   {
      this.streamSource = stream;
      setupServer(locator.getLocatorURI());
   }

   public StreamServer(InputStream stream, Connector connector) throws Exception
   {
      this.streamSource = stream;
      this.connector = connector;
      if(connector != null)
      {
         if(!connector.isStarted())
         {
            throw new IllegalStateException("Connector (" + connector + ") passed to act as stream server has not been started.");
         }
         ServerInvocationHandler invocationHandler = new Handler(connector);
         connector.addInvocationHandler("stream", invocationHandler);
         internalConnector = false;
      }
      else
      {
         throw new NullPointerException("Connector passed to act as stream server can not be null.");
      }
   }

   private String getLocatorURI() throws IOException
   {
      // check for system properties for locator values
      transport = getSystemProperty(STREAM_TRANSPORT_KEY, transport);
      try
      {
         host = getLocalHostName();
      }
      catch(UnknownHostException e)
      {
         try
         {
            InetAddress localAddress = getLocalHost();
            host = localAddress.getHostAddress();
         }
         catch(UnknownHostException e1)
         {
            log.error("Stream server could not determine local host or address.");
         }
      }

      host = getSystemProperty(STREAM_HOST_KEY, host);
      String defaultPort = "" + PortUtil.findFreePort(host);
      String sPort = getSystemProperty(STREAM_PORT_KEY, defaultPort);
      
      try
      {
         port = Integer.parseInt(sPort);
      }
      catch(NumberFormatException e)
      {
         log.error("Stream server could not convert specified port " + sPort + " to a number.");
      }

      return transport + "://" + host + ":" + port;
   }

   /**
    * Gets the locator to call back on this server to get the inputstream data.
    *
    * @return
    * @throws Exception
    */
   public String getInvokerLocator() throws Exception
   {
      String locator = null;

      if(connector != null)
      {
         locator = connector.getInvokerLocator();
      }
      return locator;
   }

   public void setupServer(String locatorURI) throws Exception
   {
      InvokerLocator locator = new InvokerLocator(locatorURI);

      connector = new Connector();
      connector.setInvokerLocator(locator.getLocatorURI());
      connector.create();

      ServerInvocationHandler invocationHandler = new Handler(connector);
      connector.addInvocationHandler("stream", invocationHandler);

      connector.start();

   }

   /**
    * Handler for accepting method calls on the input stream and perform the coresponding
    * method call on the original input stream and returning the data.
    */
   public class Handler implements ServerInvocationHandler
   {
      private Connector connector = null;

      public Handler(Connector connector)
      {
         this.connector = connector;
      }

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         Object obj = invocation.getParameter();

         // will expect the parameter to ALWAYS be of type StreamCallPaylod
         if(obj instanceof StreamCallPayload)
         {
            StreamCallPayload payload = (StreamCallPayload) obj;
            String method = payload.getMethod();

            if(StreamHandler.READ.equals(method))
            {
               int i = streamSource.read();
               return new Integer(i);
            }
            else if(StreamHandler.AVAILABLE.equals(method))
            {
               int i = streamSource.available();
               return new Integer(i);
            }
            else if(StreamHandler.CLOSE.equals(method))
            {
               streamSource.close();
               if(connector != null && internalConnector)
               {
                  connector.stop();
                  connector.destroy();
               }
            }
            else if(StreamHandler.RESET.equals(method))
            {
               streamSource.reset();
            }
            else if(StreamHandler.MARKSUPPORTED.equals(method))
            {
               boolean b = streamSource.markSupported();
               return new Boolean(b);
            }
            else if(StreamHandler.MARKREADLIMIT.equals(method))
            {
               Object[] param = payload.getParams();
               Integer intr = (Integer) param[0];
               int readLimit = intr.intValue();
               streamSource.mark(readLimit);
            }
            else if(StreamHandler.SKIP.equals(method))
            {
               Object[] param = payload.getParams();
               Long lg = (Long) param[0];
               long n = lg.longValue();
               long ret = streamSource.skip(n);
               return new Long(ret);
            }
            else if(StreamHandler.READBYTEARRAY.equals(method))
            {
               Object[] param = payload.getParams();
               byte[] byteParam = (byte[]) param[0];
               int i = streamSource.read(byteParam);
               StreamCallPayload ret = new StreamCallPayload(StreamHandler.READBYTEARRAY);
               ret.setParams(new Object[]{byteParam, new Integer(i)});
               return ret;
            }
            else
            {
               throw new Exception("Unsupported method call - " + method);
            }
         }
         else
         {
            log.error("Can not process invocation request because is not of type StreamCallPayload.");
            throw new Exception("Invalid payload type.  Must be of type StreamCallPayload.");
         }
         return null;
      }

      /**
       * Adds a callback handler that will listen for callbacks from
       * the server invoker handler.
       *
       * @param callbackHandler
       */
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         // NO OP as do not handling callback listeners in this example
      }

      /**
       * Removes the callback handler that was listening for callbacks
       * from the server invoker handler.
       *
       * @param callbackHandler
       */
      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         // NO OP as do not handling callback listeners in this example
      }

      /**
       * set the mbean server that the handler can reference
       *
       * @param server
       */
      public void setMBeanServer(MBeanServer server)
      {
         // NO OP as do not need reference to MBeanServer for this handler
      }

      /**
       * set the invoker that owns this handler
       *
       * @param invoker
       */
      public void setInvoker(ServerInvoker invoker)
      {
         // NO OP as do not need reference back to the server invoker
      }
   }

   static private String getSystemProperty(final String name, final String defaultValue)
   {
      if (SecurityUtility.skipAccessControl())
         return System.getProperty(name, defaultValue);
         
      String value = null;
      try
      {
         value = (String)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return System.getProperty(name, defaultValue);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RuntimeException) e.getCause();
      }
      
      return value;
   }
   
   static private InetAddress getLocalHost() throws UnknownHostException
   {
      if (SecurityUtility.skipAccessControl())
      {
         try
         {
            return InetAddress.getLocalHost();
         }
         catch (IOException e)
         {
            return InetAddress.getByName("127.0.0.1");
         }
      }

      try
      {
         return (InetAddress) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IOException
            {
               try
               {
                  return InetAddress.getLocalHost();
               }
               catch (IOException e)
               {
                  return InetAddress.getByName("127.0.0.1");
               }
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (UnknownHostException) e.getCause();
      }
   }
   
   static private String getLocalHostName() throws UnknownHostException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return getLocalHost().getHostName();
      }

      try
      {
         return (String) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IOException
            {
               InetAddress address = null;
               try
               {
                  address = InetAddress.getLocalHost();
               }
               catch (IOException e)
               {
                  address = InetAddress.getByName("127.0.0.1");
               }
               
               return address.getHostName();
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (UnknownHostException) e.getCause();
      }
   }
}