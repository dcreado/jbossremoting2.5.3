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

package org.jboss.test.remoting.oneway;

import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.logging.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.test.remoting.TestUtil;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:telrod@vocalocity.net">Tom Elrod</a>
 * @version $Revision: 1467 $
 */
public class OnewayInvokerServer extends ServerTestCase //implements ShutdownListener
{
   private int serverPort = 8081;
   private Connector connector = null;
   private String transport = "socket";

   private static final Logger log = Logger.getLogger(OnewayInvokerServer.class);

   public void init(Map metatdata) throws Exception
   {
      if(serverPort < 0)
      {
         serverPort = TestUtil.getRandomPort();
      }
      log.debug("port = " + serverPort);

      connector = new Connector();
      InvokerLocator locator = new InvokerLocator(buildLocatorURI(metatdata));
      connector.setInvokerLocator(locator.getLocatorURI());
      connector.create();
      connector.addInvocationHandler(getSubsystem(), getServerInvocationHandler());
      connector.start();
   }

   private String buildLocatorURI(Map metadata)
   {
      if(metadata == null || metadata.size() == 0)
      {
         return transport + "://localhost:" + serverPort;
      }
      else
      {
         StringBuffer uriBuffer = new StringBuffer(transport + "://localhost:" + serverPort);

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

   protected ServerInvocationHandler getServerInvocationHandler()
   {
      return new OnewayServerInvocationHandler();
   }

   protected String getSubsystem()
   {
      return "test";
   }

   public void setUp() throws Exception
   {
      init(null);
   }

   public void tearDown() throws Exception
   {
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   public static void main(String[] args)
   {
      OnewayInvokerServer server = new OnewayInvokerServer();
      try
      {
         server.setUp();

         Thread.currentThread().sleep(4000000);

         server.tearDown();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }


}
