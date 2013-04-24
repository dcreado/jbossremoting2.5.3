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

package org.jboss.test.remoting.performance.synchronous;

import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.logging.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.transport.Connector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class PerformanceServerTest extends ServerTestCase
{
   // default transport and port
   private String transport = "socket";
   private int serverPort = 9090;
   protected String host = "localhost";

   private Connector connector = null;

   private static final Logger log = Logger.getLogger(PerformanceServerTest.class);

   public void setHost(String host)
   {
      this.host = host;
   }

   public void init(Map metatdata) throws Exception
   {
      if(serverPort < 0)
      {
         //serverPort = Math.abs(new Random().nextInt(2000) + 2000);
         throw new Exception("port setting for server can not be null.");
      }
      log.debug("port = " + serverPort);

      connector = new Connector();
      InvokerLocator locator = new InvokerLocator(buildLocatorURI(metatdata));
      System.out.println("starting remoting server using locator of " + locator);
      log.debug("starting remoting server using locator of " + locator);
      connector.setInvokerLocator(locator.getLocatorURI());
      connector.create();
      connector.addInvocationHandler(getSubsystem(), getServerInvocationHandler());
      connector.start();
      log.info("Server started on " + locator.getLocatorURI());
   }

   private String buildLocatorURI(Map metadata)
   {
      if(metadata == null || metadata.size() == 0)
      {
         return transport + "://" + host + ":" + serverPort;
      }
      else
      {
         StringBuffer uriBuffer = new StringBuffer(transport + "://" + host + ":" + serverPort);

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
      return "performance";
   }

   protected ServerInvocationHandler getServerInvocationHandler()
   {
      return new PerformanceInvocationHandler();
   }

   public void setUp() throws Exception
   {
      String newTransport = System.getProperty(PerformanceTestCase.REMOTING_TRANSPORT);
      if(newTransport != null && newTransport.length() > 0)
      {
         transport = newTransport;
         log.info("Using transport: " + transport);
      }

      String newHost = System.getProperty(PerformanceTestCase.REMOTING_HOST);
      if(newHost != null && newHost.length() > 0)
      {
         host = newHost;
         log.info("Using host: " + host);
      }

      Map metadata = new HashMap();

      String newMetadata = System.getProperty(PerformanceTestCase.REMOTING_METADATA);
      if(newMetadata != null && newMetadata.length() > 0)
      {
         metadata.putAll(parseMetadataString(newMetadata));
         log.info("Using metadata: " + metadata);
      }

      init(metadata);
   }

   public static Map parseMetadataString(String newMetadata)
   {

      StringTokenizer tok = new StringTokenizer(newMetadata, "&");
      Map parameters = new HashMap(tok.countTokens());
      while(tok.hasMoreTokens())
      {
         String token = tok.nextToken();
         int eq = token.indexOf("=");
         String name = (eq > -1) ? token.substring(0, eq) : token;
         String value = (eq > -1) ? token.substring(eq + 1) : "";
         parameters.put(name, value);
      }
      return parameters;
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
      PerformanceServerTest test = new PerformanceServerTest();
      try
      {
         test.setUp();
         Thread.currentThread().sleep(30000000);
         test.tearDown();
         //System.exit(0);
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }
}
