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

package org.jboss.remoting.transport.local;

import org.jboss.remoting.AbstractInvoker;
import org.jboss.remoting.Client;
import org.jboss.remoting.ConnectionFailedException;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.InvokerRegistry;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.serialization.SerializationManager;
import org.jboss.remoting.serialization.SerializationStreamFactory;
import org.jboss.remoting.transport.BidirectionalClientInvoker;
import org.jboss.remoting.util.SecurityUtility;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;

/**
 * LocalClientInvoker does not use any transport protocol for invoking
 * the ServerInvoker, instead will make call directly on it locally.
 * This increases performance since no serialization required as well
 * as needed for push callbacks where InvokerCallbackHandler is in
 * same JVM as the callback server.
 *
 * @author <a href="mailto:telrod@vocalocity.net">Tom Elrod</a>
 * @version $Revision: 3956 $
 */
public class LocalClientInvoker extends AbstractInvoker implements BidirectionalClientInvoker
{
   private static final Logger log = Logger.getLogger(LocalClientInvoker.class);

   private ServerInvoker serverInvoker;
   private boolean isConnected = false;
   private boolean byValue = false;

   public LocalClientInvoker(InvokerLocator locator)
   {
      super(locator);
   }

   public LocalClientInvoker(InvokerLocator locator, Map configuration)
   {
      super(locator, configuration);
   }

   public LocalClientInvoker(InvokerLocator locator, Map configuration, boolean byValue)
   {
      super(locator, configuration);
      this.byValue = byValue;
   }

   /**
    * transport a request against a remote ServerInvoker
    *
    * @param invocation
    * @return
    * @throws Throwable
    */
   public Object invoke(InvocationRequest invocation) throws Throwable
   {
      if(log.isTraceEnabled())
      {
         log.trace("Using local client invoker for invocation.");
      }

      InvocationRequest localInvocation = invocation;

      if(byValue)
      {
         localInvocation = marshallInvocation(localInvocation);
      }

      Object ret = null;
      if(serverInvoker != null)
      {
         try
         {
            ret = serverInvoker.invoke(localInvocation);
         }
         catch (ServerInvoker.InvalidStateException invalidStateEx)
         {
            if(log.isTraceEnabled())
            {
               log.trace("Error calling on " + serverInvoker + " because is in invalid state.  Will retry with new server invoker.");
            }

            ServerInvoker newServerInvoker = null;

            // try to get new server invoker if one exists
            ServerInvoker[] invokers = InvokerRegistry.getServerInvokers();
            if(invokers != null)
            {
               for(int x = 0; x < invokers.length; x++)
               {
                  ServerInvoker svrinvoker = invokers[x];
                  InvokerLocator svrlocator = svrinvoker.getLocator();
                  if(getLocator().equals(svrlocator))
                  {
                     newServerInvoker = svrinvoker;
                     break;
                  }
               }
            }
            // if new server invoker found, try invocation call again
            if(newServerInvoker != null)
            {
               serverInvoker = newServerInvoker;
               ret = serverInvoker.invoke(localInvocation);
            }
            else
            {
               throw invalidStateEx;
            }
         }
      }
      else
      {
         throw new ConnectionFailedException("Error invoking on server because " +
                                             "no local server to call upon.");
      }

      return ret;
   }

   protected InvocationRequest marshallInvocation(InvocationRequest localInvocation) throws IOException, ClassNotFoundException
   {
      final Object param = localInvocation.getParameter();
      Object newParam = null;
      String serializationType = getSerializationType();
      final SerializationManager manager = SerializationStreamFactory.getManagerInstance(serializationType);

      if (serializationType.indexOf("jboss") < 0 || SecurityUtility.skipAccessControl())
      {
         newParam = manager.createMarshalledValueForClone(param).get();
      }
      else
      {
         try
         {
            newParam = AccessController.doPrivileged( new PrivilegedExceptionAction()
            {
               public Object run() throws Exception
               {
                  return manager.createMarshalledValueForClone(param).get();
               }
            });
         }
         catch (PrivilegedActionException e)
         {
            throw (IOException) e.getCause();
         }
      }
      
      localInvocation.setParameter(newParam);
      return localInvocation;
   }

   /**
    * subclasses must provide this method to return true if their remote connection is connected and
    * false if disconnected.  in some transports, such as SOAP, this method may always return true, since the
    * remote connectivity is done on demand and not kept persistent like other transports (such as socket-based
    * transport).
    *
    * @return boolean true if connected, false if not
    */
   public boolean isConnected()
   {
      return isConnected;
   }

   /**
    * connect to the remote invoker
    *
    * @throws ConnectionFailedException
    */
   public void connect() throws ConnectionFailedException
   {
      isConnected = true;
   }

   /**
    * disconnect from the remote invoker.  Once disconnect called
    * will not be able to re-connect by calling connect since will
    * loose reference to server invoker.
    */
   public void disconnect()
   {
      isConnected = false;
   }

   public void setMarshaller(Marshaller marshaller)
   {
      // No op since is local, do not need marshaller
   }

   public Marshaller getMarshaller()
   {
      return null;
   }

   public void setUnMarshaller(UnMarshaller unmarshaller)
   {
      // No op since is local, do not need unmarshaller
   }

   public UnMarshaller getUnMarshaller()
   {
      return null;
   }

   public void establishLease(String sessionID, Map configuration, long leasePeriod) throws Throwable
   {
      // noop since is local
   }

   public void terminateLease(String sessionId, int disconnectTimeout)
   {
      // noop since is local
   }

   public long getLeasePeriod(String sessionID)
   {
      return -1; // no lease, since is local
   }

   /**
    * This will set the local reference to the server invoker.
    * This is needed to so can make calls directly against server.
    *
    * @param svrInvoker
    */
   public void setServerInvoker(ServerInvoker svrInvoker)
   {
      this.serverInvoker = svrInvoker;
   }

   public InvokerLocator getCallbackLocator(Map metadata)
   {
      String transport = (String) metadata.get(Client.CALLBACK_SERVER_PROTOCOL);
      String host = (String) metadata.get(Client.CALLBACK_SERVER_HOST);
      String sPort = (String) metadata.get(Client.CALLBACK_SERVER_PORT);
      int port = -1;
      if (sPort != null)
      {
         try
         {
            port = Integer.parseInt(sPort);
         }
         catch (NumberFormatException e)
         {
            throw new RuntimeException("Can not set internal callback server port as configuration value (" + sPort + " is not a number.");
         }
      }

      return new InvokerLocator(transport, host, port, "callback", metadata);
   }
}
