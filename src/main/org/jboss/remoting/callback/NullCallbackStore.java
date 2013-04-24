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

package org.jboss.remoting.callback;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import org.jboss.logging.Logger;
import org.jboss.remoting.SerializableStore;

/**
 * This implementation does nothing other than throw away persisted objects and throw exceptions.
 * This is to be use when don't have a proper store or don't care about throwing away callbacks when
 * starting to run out of memory.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class NullCallbackStore implements SerializableStore, Serializable
{
   static final long serialVersionUID = -8182007953992756845L;

   private boolean isCallbackLost = false;

   private static final Logger log = Logger.getLogger(NullCallbackStore.class);

   /**
    * Getst the number of objects stored and available.
    *
    * @return
    */
   public int size()
   {
      return isCallbackLost ? 1 : 0;
   }

   /**
    * Will look through the files in the store directory for the oldest object serialized to disk, load it,
    * delete the file, and return the deserialized object.
    * Important to note that once this object is returned from this method, it is gone forever from this
    * store and will not be able to retrieve it again without adding it back.
    *
    * @return
    * @throws java.io.IOException
    */
   public Object getNext() throws IOException
   {
      if(isCallbackLost)
      {
         isCallbackLost = false;
         return new FailedCallback("This is an invalid callback.  The server ran out of memory, so callbacks were lost.");
      }
      else
      {
         return null;
      }
   }

   /**
    * Persists the serializable object passed to the directory specified.  The file name will be the current time
    * in milliseconds (vis System.currentTimeMillis()) with the specified suffix.  This object can later be
    * retrieved using the getNext() method, but objects will be returned in the order that they were added (FIFO).
    *
    * @param object
    * @throws java.io.IOException
    */
   public void add(Serializable object) throws IOException
   {
      isCallbackLost = true;
      log.debug("Lost callback because not enough free memory available.  Callback lost was " + object);
      throw new IOException("Callback has been lost because not enough free memory to hold object.");
   }

   /**
    * No op
    *
    * @param config
    */
   public void setConfig(Map config)
   {
   }

   /**
    * No op
    *
    * @throws Exception
    */
   public void start() throws Exception
   {
   }

   /**
    * No op
    */
   public void stop()
   {
   }

   /**
    * This is a no op method, but needed in order to be used as a service within JBoss AS.
    *
    * @throws Exception
    */
   public void create() throws Exception
   {
   }

   /**
    * This is a no op method, but needed in order to be used as a service within JBoss AS.
    */
   public void destroy()
   {
   }

   /**
    * Sets if store should clean up persisted files when shutdown (destroy()).
    *
    * @param purgeOnShutdown
    */
   public void setPurgeOnShutdown(boolean purgeOnShutdown)
   {
   }

   /**
    * Returns if store will clean up persisted files when shutdown (destroy()).
    *
    * @return
    */
   public boolean getPurgeOnShutdown()
   {
      return false;
   }

   public void purgeFiles()
   {
   }

   public class FailedCallback extends Callback
   {

      public FailedCallback(Object callbackPayload)
      {
         super(callbackPayload);
      }

      public Object getCallbackObject()
      {
         throw new RuntimeException("This is an invalid callback.  The server ran out of memory, so callbacks were lost.");
      }
   }
}
