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


/**
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Mar 13, 2008
 * </p>
 */
public class MockEJBClient extends SoakConstants implements Runnable
{
   protected static Logger log = Logger.getLogger(MockEJBClient.class);
   
   protected InvokerLocator locator;
   protected Map metadata;
   protected String name;
   protected boolean ok = true;
   protected Set inUseSet;
   protected ClientLauncher.Counter failureCounter;
   
   public MockEJBClient(String locator, Map metadata) throws Exception
   {
      this.locator = new InvokerLocator(locator);
      this.metadata = metadata;
      this.name = (String) metadata.get(SoakConstants.NAME);
      this.inUseSet = (Set) metadata.remove(IN_USE_SET);
      inUseSet.add(this);
      failureCounter = (ClientLauncher.Counter) metadata.remove(FAILURE_COUNTER);
      log.info("created " + name);
   }

   public void run()
   {
      try
      {
         String s = (String) metadata.get(SoakConstants.NUMBER_OF_CALLS);
         int calls = Integer.valueOf(s).intValue();
         for (int i = 0; i < calls; i++)
         {
            makeCall(i);
         }
      }
      catch (Throwable t)
      {
         log.error(name, t);
      }
      finally
      {
         inUseSet.remove(this);
         log.info(name + ": " + (ok ? "PASS" : "FAIL"));
         if (!ok) failureCounter.increment();
      }
   }

   protected void makeCall(int i) throws Throwable
   {
      Client client = null;

      try
      {
         client = new Client(locator);
         client.connect();
         Map metadata = new HashMap();
         metadata.put(PAYLOAD, "abc");
         Object response = client.invoke(COPY, metadata);
         if (!"abc".equals(response))
         {
            ok = false;
            log.info(name + ": failure on call " + i);
         }
      }
      catch (Throwable t)
      {
         ok = false;
         log.info(name + ": failure on call " + i);
         throw t;
      }
      finally
      {
         client.disconnect();
      }
   }
   
   public String toString()
   {
      return name;
   }
}

