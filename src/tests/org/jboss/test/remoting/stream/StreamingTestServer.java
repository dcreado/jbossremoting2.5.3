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

package org.jboss.test.remoting.stream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import javax.management.MBeanServer;

import org.apache.log4j.Logger;
import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.stream.StreamInvocationHandler;
import org.jboss.remoting.transport.Connector;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class StreamingTestServer extends ServerTestCase
{
   private static Logger log = Logger.getLogger(StreamingTestServer.class);
   
   // Default locator values
   private static String transport = "socket";
   private static String host = "localhost";
   private static int port = 5400;

   private String locatorURI;
   private Connector connector = null;

   public void setupServer() throws Exception
   {
      String bindAddr = System.getProperty("jrunit.bind_addr", host);
      locatorURI = transport + "://" + bindAddr + ":" + port; 
      InvokerLocator locator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      connector = new Connector();
      connector.setInvokerLocator(locator.getLocatorURI());
      connector.create();

      TestStreamInvocationHandler invocationHandler = new TestStreamInvocationHandler();
      // first parameter is sub-system name.  can be any String value.
      connector.addInvocationHandler("test_stream", invocationHandler);

      connector.start();
      log.info("Started remoting server with locator uri of: " + locatorURI);
   }

   protected void setUp() throws Exception
   {
      setupServer();
   }

   protected void tearDown() throws Exception
   {
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   /**
    * Can pass transport and port to be used as parameters.
    * Valid transports are 'rmi' and 'socket'.
    *
    * @param args
    */
   public static void main(String[] args)
   {
      if(args != null && args.length == 3)
      {
         transport = args[0];
         host = args[1];
         port = Integer.parseInt(args[2]);
      }

      StreamingTestServer server = new StreamingTestServer();
      try
      {
         server.setUp();

         // sleep the thread for 10 seconds while waiting for client to call
         Thread.sleep(10000);

         server.tearDown();
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }

   /**
    * Simple invocation handler implementation.
    */
   public static class TestStreamInvocationHandler implements StreamInvocationHandler
   {
      private InputStream stream = null;

      private int streamSize = 0;

      /**
       * called to handle a specific invocation
       *
       * @param invocation
       * @return
       * @throws Throwable
       */
      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         // Print out the invocation request
         log.info("Invocation request is: " + invocation.getParameter());

         // Just going to return static string as this is just simple example code.
         return new Integer(streamSize);
      }

      /**
       * Adds a callback handler that will listen for callbacks from
       * the server invoker handler.
       *
       * @param callbackHandler
       */
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         // NO OP as do not handling callback listeners in this example
      }

      /**
       * Removes the callback handler that was listening for callbacks
       * from the server invoker handler.
       *
       * @param callbackHandler
       */
      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         // NO OP as do not handling callback listeners in this example
      }

      /**
       * set the mbean server that the handler can reference
       *
       * @param server
       */
      public void setMBeanServer(MBeanServer server)
      {
         // NO OP as do not need reference to MBeanServer for this handler
      }

      /**
       * set the invoker that owns this handler
       *
       * @param invoker
       */
      public void setInvoker(ServerInvoker invoker)
      {
         // NO OP as do not need reference back to the server invoker
      }

      public Object handleStream(InputStream stream, InvocationRequest param)
      {
         this.stream = stream;

         try
         {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte buf[] = new byte[4096];
            while(true)
            {
               int c = this.stream.read(buf);
               if(c < 0)
               {
                  break;
               }
               out.write(buf, 0, c);
            }
            byte[] bytes = out.toByteArray();
            streamSize = bytes.length;
            log.info("Read stream.  Contents is: " + new String(bytes));
         }
         catch(IOException e)
         {
            e.printStackTrace();
         }
         finally
         {
            try
            {
               stream.close();
            }
            catch(IOException e)
            {
               e.printStackTrace();
            }
         }
         // build return map
         Map retMap = new HashMap();
         retMap.put("subsystem", param.getSubsystem());
         retMap.put("clientid", param.getSessionId());
         retMap.put("paramval", param.getParameter());
         return retMap;
      }
   }

}