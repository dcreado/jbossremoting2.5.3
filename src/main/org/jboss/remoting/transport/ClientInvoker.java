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

package org.jboss.remoting.transport;

import org.jboss.remoting.ConnectionFailedException;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.Invoker;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.UnMarshaller;

import javax.net.SocketFactory;
import java.util.Map;
import java.util.List;

/**
 * Interface to be used for calling on all the different invoker types (LocalClientInvoker
 * and RemoteClientInvoker).
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 */
public interface ClientInvoker extends Invoker
{
   /**
    * This should be set when want to override the default behavior of automatically getting a
    * suitable locator. This should be used want want to control what type of callbacks to receive
    * (pull or push).  Set to null to poll for callback messages. This can also be used to receive
    * callbacks using another transport and subsystem, if desired.
    *
    * @return id for callback handler and locator combination.
    */
   String addClientLocator(String sessionId,
                           InvokerCallbackHandler callbackhandler,
                           InvokerLocator locator);

   /**
    * Gets the client locator.  This locator will be used by the server side
    * to make callbacks to the handler for this locator.
    */
   InvokerLocator getClientLocator(String listenerId);

   /**
    * Gets list of AbstractInvoker.CallbackLocatorHolder containing listener id and locator
    */
   List getClientLocators(String sessionId, InvokerCallbackHandler handler);

   /**
    * Gets SocketFactory used to connect to ServerInvoker.
    */
   SocketFactory getSocketFactory();

   /**
    * Sets the SocketFactory that will be used to connect to ServerInvoker.
    *
    * @param socketFactory
    */
   void setSocketFactory(SocketFactory socketFactory);

   /**
    * Transport a request against a remote ServerInvoker.
    */
   Object invoke(InvocationRequest in) throws Throwable;

   /**
    * Subclasses must provide this method to return true if their remote connection is connected and
    * false if disconnected.  In some transports, such as SOAP, this method may always return true,
    * since the remote connectivity is done on demand and not kept persistent like other transports
    * (such as socket-based transport).
    *
    * @return boolean true if connected, false if not.
    */
   boolean isConnected();

   /**
    * Connect to the remote invoker.
    */
   void connect() throws ConnectionFailedException;

   /**
    * Disconnect from the remote invokere.
    */
   void disconnect();

   void setMarshaller(Marshaller marshaller);

   Marshaller getMarshaller();

   void setUnMarshaller(UnMarshaller unmarshaller);

   UnMarshaller getUnMarshaller();

   void establishLease(String sessionID, Map configuration, long leasePeriod) throws Throwable;

   /**
    * Must behave as a noop if there's no active lease.
    */
   void terminateLease(String sessionID, int disconnectTimeout);

   /**
    * @return the lease period (in ms) if the client has an active leasing mechanism with the server
    *         or -1 otherwise.
    */
   long getLeasePeriod(String sessionID);

}