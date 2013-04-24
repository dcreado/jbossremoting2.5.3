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
package org.jboss.test.remoting.soak;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.bisocket.Bisocket;

/**
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Mar 13, 2008
 * </p>
 */
public class MockJBMClient extends SoakConstants implements Runnable
{
   protected static Logger log = Logger.getLogger(MockJBMClient.class);
   
   protected InvokerLocator locator;
   protected Map metadata;
   protected String name;
   protected boolean ok = true;
   protected Client client;
   protected Set inUseSet;
   protected ClientLauncher.Counter failureCounter;
   
   public MockJBMClient(String locator, Map metadata) throws Exception
   {
      this.locator = new InvokerLocator(locator);
      this.metadata = metadata;
      this.name = (String) metadata.get(NAME);
      this.inUseSet = (Set) metadata.remove(IN_USE_SET);
      inUseSet.add(this);
      failureCounter = (ClientLauncher.Counter) metadata.remove(FAILURE_COUNTER);
      log.info("created " + name);
   }
   
   public void run()
   {
      try
      {
         client = new Client(locator);
         client.connect();
         TestCallbackHandler callbackHandler = new TestCallbackHandler();
         Map callbackMetadata = new HashMap();
         if ("bisocket".equals(locator.getProtocol()))
         {
            callbackMetadata.put(Bisocket.IS_CALLBACK_SERVER, "true");
         }
         client.addListener(callbackHandler, callbackMetadata, null, true);
         log.debug(client.getSessionId() + ": added callback listener");
         String s = (String) metadata.get(NUMBER_OF_CALLS);
         int calls = Integer.valueOf(s).intValue();
         for (int i = 0; i < calls; i++)
         {
            makeCall(i);
         }
         client.removeListener(callbackHandler);
         client.disconnect();
         s = (String) metadata.get(NUMBER_OF_CALLBACKS);
         int i = Integer.parseInt(s);
         boolean ok = callbackHandler.counter == i * calls;
         if (!ok)
         {
            log.info("expected: " + (i * calls) + ", received: " + callbackHandler.counter);
         }
      }
      catch (Throwable t)
      {
         log.error(name, t);
         ok = false;
      }
      finally
      {
         client.disconnect();
         log.info(name + ": " + (ok ? "PASS" : "FAIL"));
         inUseSet.remove(this);
         if (!ok) failureCounter.increment();
      }
   }
   
   protected void makeCall(int i) throws Throwable
   {
      client.invoke(CALLBACK, metadata);
   }
   
   static class TestCallbackHandler implements InvokerCallbackHandler
   {
      int counter;
      
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         counter++;
         log.debug("received callback");
      }  
   }
   
   public String toString()
   {
      return name;
   }
}

