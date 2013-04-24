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

package org.jboss.test.remoting.transport.rmi;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.logging.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.rmi.RMIServerInvoker;
import org.jboss.test.remoting.TestUtil;
import org.jboss.test.remoting.performance.synchronous.PerformanceServerTest;
import org.jboss.test.remoting.performance.synchronous.PerformanceTestCase;
import org.jboss.test.remoting.transport.mock.MockServerInvocationHandler;


/**
 * This is the concrete test for invoker server.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class RMIInvokerNativeMarshallerServerTest extends ServerTestCase
{
   private int port = RMIServerInvoker.DEFAULT_REGISTRY_PORT - 1;
   protected String transport = "rmi";
   private Connector connector;

   private static final Logger log = Logger.getLogger(RMIInvokerNativeMarshallerServerTest.class);

   public void init(Map metatdata) throws Exception
   {
      if(port < 0)
      {
         port = TestUtil.getRandomPort();
      }
      log.debug("port = " + port);

      connector = new Connector();
      InvokerLocator locator = new InvokerLocator(buildLocatorURI(metatdata));
      System.out.println("server locator: " + locator);
      connector.setInvokerLocator(locator.getLocatorURI());
      connector.start();
      connector.addInvocationHandler(getSubsystem(), getServerInvocationHandler());
   }

   private String buildLocatorURI(Map metadata)
   {
      if(metadata == null || metadata.size() == 0)
      {
         return transport + "://localhost:" + port;
      }
      else
      {
         StringBuffer uriBuffer = new StringBuffer(transport + "://localhost:" + port);

         Set keys = metadata.keySet();
         if(keys.size() > 0)
         {
            uriBuffer.append("/?");
         }

         Iterator itr = keys.iterator();
         while(itr.hasNext())
         {
            String key = (String) itr.next();
            String value = (String) metadata.get(key);
            uriBuffer.append(key + "=" + value + "&");
         }
         return uriBuffer.substring(0, uriBuffer.length() - 1);
      }
   }

   protected String getSubsystem()
   {
      return "mock";
   }

   protected ServerInvocationHandler getServerInvocationHandler()
   {
      return new MockServerInvocationHandler();
   }

   public void setUp() throws Exception
   {
      Map metadata = new HashMap();
      String newMetadata = System.getProperty(PerformanceTestCase.REMOTING_METADATA);
      if(newMetadata != null && newMetadata.length() > 0)
      {
         metadata.putAll(PerformanceServerTest.parseMetadataString(newMetadata));
      }
      metadata.put(RMIServerInvoker.REGISTRY_PORT_KEY, String.valueOf(port + 1));
      addMetadata(metadata);
      init(metadata);
   }

   public void tearDown() throws Exception
   {
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }
   
   protected void addMetadata(Map metadata)
   {
   }

   public static void main(String[] args)
   {
      RMIInvokerNativeMarshallerServerTest server = new RMIInvokerNativeMarshallerServerTest();
      try
      {
         server.setUp();
         Thread.currentThread().sleep(6000000);
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }


}
