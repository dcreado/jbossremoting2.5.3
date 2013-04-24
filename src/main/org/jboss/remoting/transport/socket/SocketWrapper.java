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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public abstract class SocketWrapper
{
   // Constants ------------------------------------------------------------------------------------

   private static final Logger log = Logger.getLogger(SocketWrapper.class);

   public static final String MARSHALLER = "marshaller";
   public static final String UNMARSHALLER = "unmarshaller";
   public static final String TEMP_TIMEOUT = "temptimeout";
   public static final String WRITE_TIMEOUT = "writeTimeout";
   
   protected static final int CLOSING = 254;
   
   // Static ---------------------------------------------------------------------------------------

   private static boolean trace = log.isTraceEnabled();

   // Attributes -----------------------------------------------------------------------------------

   protected Socket socket;
   private int timeout;

   // Constructors ---------------------------------------------------------------------------------

   protected SocketWrapper(Socket socket)
   {
      if (trace) { log.trace("constructing " + getClass().getName() + " instance for " + socket); }
      this.socket = socket;
   }

   protected SocketWrapper(Socket socket, Integer timeoutInt) throws SocketException
   {
      if (trace) { log.trace("constructing " + getClass().getName() + " instance for " + socket + ", using timeout " + timeoutInt); }
      this.socket = socket;

      if(timeoutInt != null)
      {
         this.timeout = timeoutInt.intValue();
         setTimeout(this.timeout);
      }
   }

   // Public ---------------------------------------------------------------------------------------

   public void setTimeout(int timeout) throws SocketException
   {
      if (trace) { log.trace(this + " setting timeout to " + timeout); }
      this.timeout = timeout;
      if(socket != null)
      {
         socket.setSoTimeout(timeout);
      }
   }

   public int getTimeout()
   {
      return timeout;
   }

   public void close() throws IOException
   {      
      if(socket != null)
      {
         log.trace(this + " closing socket");
         socket.close();
         log.trace(this + " closed socket");
      }
   }
   
   public boolean isClosed()
   {
      if (socket == null)
         return false;
      
      return socket.isClosed();
   }

   public Socket getSocket()
   {
      return socket;
   }

   public abstract OutputStream getOutputStream() throws IOException;

   public abstract InputStream getInputStream() throws IOException;

   public abstract void checkConnection() throws IOException;

   // Package protected ----------------------------------------------------------------------------

   // Protected ------------------------------------------------------------------------------------

   protected void finalize()
   {
      if(socket != null)
      {
         try
         {
            socket.close();
         }
         catch(Exception e)
         {
            log.debug(this + " failed to close socket", e);
         }
      }
   }

   // Private --------------------------------------------------------------------------------------

   // Inner classes --------------------------------------------------------------------------------

}