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

package org.jboss.test.remoting.transport.mock;

import java.io.IOException;
import java.util.Map;
import org.jboss.remoting.ConnectionFailedException;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.RemoteClientInvoker;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.UnMarshaller;

/**
 * MockClientInvoker
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @version $Revision: 566 $
 */
public class MockClientInvoker extends RemoteClientInvoker
{
   private final MockServerInvoker server;
   private boolean loaded = false;

   public MockClientInvoker(InvokerLocator locator, MockServerInvoker server)
   {
      super(locator);
      this.server = server;
   }

   /**
    * subclasses must implement this method to provide a hook to connect to the remote server, if this applies
    * to the specific transport. However, in some transport implementations, this may not make must difference since
    * the connection is not persistent among invocations, such as SOAP.  In these cases, the method should
    * silently return without any processing.
    *
    * @throws ConnectionFailedException
    */
   protected void handleConnect()
         throws ConnectionFailedException
   {
   }

   /**
    * subclasses must implement this method to provide a hook to disconnect from the remote server, if this applies
    * to the specific transport. However, in some transport implementations, this may not make must difference since
    * the connection is not persistent among invocations, such as SOAP.  In these cases, the method should
    * silently return without any processing.
    */
   protected void handleDisconnect()
   {
   }


   public void connect()
         throws ConnectionFailedException
   {
   }

   public void disconnect()
   {
   }

   protected String getDefaultDataType()
   {
      return null;
   }

   public boolean isConnected()
   {
      return false;
   }

   protected Object transport(String sessionId, Object invocation, Map metadata, Marshaller marshaller, UnMarshaller unmarshaller)
         throws IOException, ConnectionFailedException
   {
      return server.invoke(invocation);
   }
}
