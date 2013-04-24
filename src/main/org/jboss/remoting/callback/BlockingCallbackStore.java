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

import org.jboss.logging.Logger;
import org.jboss.remoting.SerializableStore;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This callback store does not persist callback messages when memory is
 * running low, but instead will block the thread making the handle callback
 * call from the server invoker.  The intention is that this will throttle the
 * server invoker from generating/sending any more callbacks until client as
 * called to get the in-memory callbacks that have already been collected and
 * memory has been released.
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class BlockingCallbackStore implements SerializableStore
{
   private Object lockObject = new Object();

   private List callbacks = new ArrayList();

   private static final Logger log = Logger.getLogger(BlockingCallbackStore.class);

   /**
    * Gets the number of callbacks that are waiting
    * to be processed.
    * @return
    */
   public int size()
   {
      return callbacks.size();
   }

   /**
    * Will get the first callback that are being held for the
    * client.  This will also release the lock that is holding
    * any threads that have called the add(Serializalbe) method.
    * @return
    * @throws IOException
    */
   public Object getNext() throws IOException
   {
      Object callback = null;

      synchronized(lockObject)
      {
         if(callbacks.size() > 0)
         {
            callback = callbacks.remove(0);
         }
         lockObject.notify();
      }
      return callback;
   }

   /**
    * To be used for adding a callback to the store.
    * The thread making the call will be held until the
    * getNext() method is called.
    * @param object
    * @throws IOException
    */
   public void add(Serializable object) throws IOException
   {
      if(log.isTraceEnabled())
      {
         log.trace("Adding " + object + " to blocking callback store.  Calling thread " +
                   Thread.currentThread() + " will be held until getNext() is called");
      }

      synchronized(lockObject)
      {
         try
         {
            callbacks.add(object);
            lockObject.wait();
         }
         catch (InterruptedException e)
         {
            log.debug("InterruptedException received while waiting for thread (" +
                      Thread.currentThread() + ") to be released from BlockingCallbackStore.add(Serializable) call.");
         }
      }
   }

   /**
    * No op
    * @param config
    */
   public void setConfig(Map config)
   {
   }

   /**
    * No op
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
    * No op
    * @throws Exception
    */
   public void create() throws Exception
   {
   }

   /**
    * No op
    */
   public void destroy()
   {
   }

   /**
    * No op
    * @param purgeOnShutdown
    */
   public void setPurgeOnShutdown(boolean purgeOnShutdown)
   {
   }

   /**
    * No op.  Always returns false.
    * @return
    */
   public boolean getPurgeOnShutdown()
   {
      return false;
   }

   /**
    * No op.
    */
   public void purgeFiles()
   {
   }
}