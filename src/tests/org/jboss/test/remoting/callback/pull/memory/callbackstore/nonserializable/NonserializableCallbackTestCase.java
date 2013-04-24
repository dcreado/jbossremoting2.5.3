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
package org.jboss.test.remoting.callback.pull.memory.callbackstore.nonserializable;

import java.io.NotSerializableException;
import java.util.HashMap;

import org.jboss.logging.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.CallbackStore;

import junit.framework.TestCase;

/** 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1483 $
 * <p>
 * Copyright Oct 11, 2006
 * </p>
 */
public class NonserializableCallbackTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(NonserializableCallbackTestCase.class);
   private boolean passes = false;
   
   public void testNonserializableCallbackJavaSerialization()
   {
      log.info("entering " + getName());
      CallbackStore store = null;
      
      try
      {
         store = new CallbackStore();
         HashMap config = new HashMap();
         String path = getClass().getResource(".").getPath();
         config.put(CallbackStore.FILE_PATH_KEY, path);
         store.setConfig(config);
         store.start();
         
         Callback callback = new Callback(new NonserializablePayload(7));
         
         try
         {
            store.add(callback);
            fail();
         }
         catch (NotSerializableException e)
         {
            log.info("got expected exception");
         }
         catch (Exception e)
         {
            log.error(e);
            e.printStackTrace();
            fail();
         }
         
         passes = true;
      }
      catch (Throwable t)
      {
         log.error(t);
         t.printStackTrace();
         fail();
      }
      finally
      {
         store.purgeFiles();
         store.stop();
         
         if (passes)
            log.info(getName() + " PASSES");
         else
            log.info(getName() + " FAILS");
      }
   }
   
   
   public void testNonserializableCallbackJBossSerialization()
   {
      log.info("entering " + getName());
      CallbackStore store = null;
      
      try
      {
         store = new CallbackStore();
         HashMap config = new HashMap();
         String path = getClass().getResource(".").getPath();
         config.put(CallbackStore.FILE_PATH_KEY, path);
         config.put(InvokerLocator.SERIALIZATIONTYPE, "jboss");
         store.setConfig(config);
         store.start();
         
         Object payload = new NonserializablePayload(11);
         Callback callback = new Callback(payload);
         
         try
         {
            store.add(callback);
         }
         catch (Exception e)
         {
            log.error(e);
            e.printStackTrace();
            fail();
         }
         
         Callback retrievedCallback = (Callback) store.getNext();
         Object retrievedPayload = retrievedCallback.getParameter();
         assertTrue(payload.equals(retrievedPayload));
         
         passes = true;
      }
      catch (Throwable t)
      {
         log.error(t);
         t.printStackTrace();
         fail();
      }
      finally
      {
         store.purgeFiles();
         store.stop();
         
         if (passes)
            log.info(getName() + " PASSES");
         else
            log.info(getName() + " FAILS");
      }
   }

}
