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
import java.io.Serializable;

/**
 * This class encapsulates all the required information for a client to
 * establish a connection with the server.
 * <p/>
 * It also attempts to provide a fast hash() function since this object
 * is used as a key in a hashmap mainted by the ConnectionManager.
 *
 * @author <a href="mailto:hiram.chirino@jboss.org">Hiram Chirino</a>
 * @version $Revision: 3186 $
 */
public class ServerAddress implements Serializable
{
   /**
    * The serialVersionUID @since 1.1.4.1
    */
   private static final long serialVersionUID = -7206359745950445445L;

   /**
    * Address of host ot connect to
    */
   public String address;

   /**
    * Port the service is listening on
    */
   public int port;

   /**
    * If the TcpNoDelay option should be used on the socket.
    */
   public boolean enableTcpNoDelay = false;

   /**
    * Timeout of setSoTimeout
    */
   public int timeout = 60000;
   
   /**
    * Maximum size of connection pool
    */
   public int maxPoolSize;

   /**
    * This object is used as a key in a hashmap,
    * so we precompute the hascode for faster lookups.
    */
   private transient int hashCode;

   public ServerAddress(String address, int port,
                        boolean enableTcpNoDelay, int timeout,
                        int maxPoolSize)
   {
      this.address = address;
      this.port = port;
      this.enableTcpNoDelay = enableTcpNoDelay;
      this.hashCode = address.hashCode() + port;
      if (enableTcpNoDelay)
      {
         this.hashCode ++;
      }
      if(timeout >= 0)
      {
         this.timeout = timeout;
      }
      this.hashCode = 7 * this.hashCode + timeout;
      this.maxPoolSize = maxPoolSize;
      this.hashCode = 11 * this.hashCode + maxPoolSize;
   }

   public String toString()
   {
      return "ServerAddress[" + address + ":" + port +
         (enableTcpNoDelay ? ", enableTcpNoDelay" : ", NO enableTcpNoDelay") +
         " timeout " + timeout + " ms" + ", maxPoolSize=" + maxPoolSize + "]";
   }

   public boolean equals(Object obj)
   {
      try
      {
         // Compare this to obj
         ServerAddress o = (ServerAddress)obj;

         if (port != o.port)
         {
            return false;
         }

         if (!address.equals(o.address))
         {
            return false;
         }

         if (enableTcpNoDelay != o.enableTcpNoDelay)
         {
            return false;
         }

         if (timeout != o.timeout)
         {
            return false;
         }
         
         if (maxPoolSize != o.maxPoolSize)
         {
            return false;
         }

         return true;
      }
      catch (Throwable e)
      {
         return false;
      }
   }

   public int hashCode()
   {
      return hashCode;
   }

   /**
    * Create the transient hashCode
    *
    * @param in
    * @throws IOException
    * @throws ClassNotFoundException
    */
   private void readObject(java.io.ObjectInputStream in)
         throws IOException, ClassNotFoundException
   {
      // Trigger default serialization
      in.defaultReadObject();
      // Build the hashCode
      this.hashCode = address.hashCode() + port;
      if (enableTcpNoDelay)
      {
         this.hashCode ++;
      }
      this.hashCode = 7 * this.hashCode + timeout;
   }


}
