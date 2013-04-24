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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.PreferredStreamMarshaller;
import org.jboss.remoting.marshal.PreferredStreamUnMarshaller;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class ClientSocketWrapper extends SocketWrapper implements OpenConnectionChecker
{
   // Constants ------------------------------------------------------------------------------------

   private static final Logger log = Logger.getLogger(ClientSocketWrapper.class);

   // Static ---------------------------------------------------------------------------------------

   private static boolean trace = log.isTraceEnabled();

   // Attributes -----------------------------------------------------------------------------------

   private InputStream in;
   private OutputStream out;
   private int writeTimeout = -1;

   // Constructors ---------------------------------------------------------------------------------

   public ClientSocketWrapper(Socket socket) throws IOException
   {
      super(socket);
      createStreams(socket, null);
   }

   public ClientSocketWrapper(Socket socket, Map metadata, Integer timeout) throws Exception
   {
      super(socket, timeout);
      createStreams(socket, metadata);
   }

   // SocketWrapper overrides ----------------------------------------------------------------------

   public OutputStream getOutputStream()
   {
      return out;
   }

   public InputStream getInputStream()
   {
      return in;
   }

   public int getWriteTimeout()
   {
      return writeTimeout;
   }

   public void setWriteTimeout(int writeTimeout)
   {
      this.writeTimeout = writeTimeout;
   }

   public void checkConnection() throws IOException
   {
      // Test to see if socket is alive by send ACK message
      final byte ACK = 1;
      
//      out.reset();
//      out.writeByte(ACK);
//      out.flush();
//      in.readByte();

      out.write(ACK);
      out.flush();
      int i = in.read();
      if (trace) { log.trace(this + " got " + i + " while checking connection"); }
   }
   
   // OpenConnectionChecker implementation ---------------------------------------------------------
   
   public void checkOpenConnection() throws IOException
   {
      if (trace) log.trace("checking open connection");
      if (in.available() > 1)
      {
         log.trace("remote endpoint has closed");
         throw new IOException("remote endpoint has closed");
      }
   }

   // Public ---------------------------------------------------------------------------------------

   public String toString()
   {
      Socket socket = getSocket();
      return "ClientSocketWrapper[" + socket + "." +
         Integer.toHexString(System.identityHashCode(socket)) + "]";
   }

   // Package protected ----------------------------------------------------------------------------

   // Protected ------------------------------------------------------------------------------------

   protected void createStreams(Socket socket, Map metadata) throws IOException
   {

      String serializationType = "java"; // hardcoding to default to java serialization

      if(metadata != null)
      {
         String serializationTypeParam = (String) metadata.get(InvokerLocator.SERIALIZATIONTYPE);
         if(serializationTypeParam == null || serializationTypeParam.length() == 0)
         {
            serializationTypeParam = (String) metadata.get(InvokerLocator.SERIALIZATIONTYPE_CASED);
         }
         if(serializationTypeParam != null && serializationTypeParam.length() > 0)
         {
            serializationType = serializationTypeParam;
         }
      }

      Marshaller marshaller = null;
      UnMarshaller unmarshaller = null;
      int tempTimeout = -1;
      int savedTimeout = getTimeout();
      
      if (metadata != null)
      {
         marshaller = (Marshaller) metadata.get(MARSHALLER);
         unmarshaller = (UnMarshaller) metadata.get(UNMARSHALLER);
         Object o = metadata.get(TEMP_TIMEOUT);
         if (o instanceof Integer)
         {
            tempTimeout = ((Integer) o).intValue();
            if (tempTimeout != -1)
            {
               socket.setSoTimeout(tempTimeout);
               log.trace("set temp timeout to: " + tempTimeout);
            }
         }
         o = metadata.get(WRITE_TIMEOUT);
         if (o instanceof Integer)
         {
            writeTimeout = ((Integer) o).intValue();
            if (writeTimeout != -1)
            {
               log.trace("set writeTimeout to: " + writeTimeout);
            }
         }
      }
      
      out = createOutputStream(serializationType, socket, marshaller);
      in = createInputStream(serializationType, socket, unmarshaller);
      setTimeout(savedTimeout);
      log.trace("reset timeout: " + savedTimeout);
   }

   protected InputStream createInputStream(String serializationType, Socket socket, UnMarshaller unmarshaller)
         throws IOException
   {
      if (trace) { log.trace(this + " getting input stream from " + socket + ", " + unmarshaller); }
      
      if (unmarshaller == null)
         log.warn("got null unmarshaller");
      
      InputStream is = socket.getInputStream();
      if (unmarshaller instanceof PreferredStreamUnMarshaller)
      {
         PreferredStreamUnMarshaller psum = (PreferredStreamUnMarshaller) unmarshaller;
         is = psum.getMarshallingStream(is);
      }
      
      return is;
   }

   protected OutputStream createOutputStream(String serializationType, Socket socket, Marshaller marshaller)
         throws IOException
   {
      if (trace) { log.trace(this + " getting output stream from " + socket + ", " + marshaller); }
      
      
      if (marshaller == null)
         log.warn("got null marshaller");
      
      OutputStream os = socket.getOutputStream();
      if (writeTimeout > 0)
      {
         os = new TimedOutputStream(os, writeTimeout);
      }
      
      if (marshaller instanceof PreferredStreamMarshaller)
      {
         PreferredStreamMarshaller psm = (PreferredStreamMarshaller) marshaller;
         os = psm.getMarshallingStream(os);
      }
      
      return os;
   }

   // Private --------------------------------------------------------------------------------------

   // Inner classes --------------------------------------------------------------------------------

}
