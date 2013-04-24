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
package org.jboss.test.remoting.transport.socket.socketpool;

import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.logging.Logger;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;

import javax.management.MBeanServer;

/** 
 * See SocketPoolTestCase for description.
 *  
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 * 
 * @version $Revision: 2911 $
 * <p>
 * Copyright Nov 2, 2006
 * </p>
 */
public class SocketPoolTestServer extends ServerTestCase
{
   public final static int NUMBER_OF_CALLS = 5;
   private static Logger log = Logger.getLogger(SocketPoolTestServer.class);
   private static Object lock = new Object();
   private static Object stopLock = new Object();
   private static int callCounter;
   private static boolean done;
   
   // Default locator values
   private static String transport = "socket";
   private static String host = "localhost";
   private static int port = 6413;

   private Connector connector = null;

   public void setupServer(String locatorURI) throws Exception
   {
      log.warn("EXCEPTIONS ARE EXPECTED");
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      
      InvokerLocator locator = new InvokerLocator(locatorURI);
      connector = new Connector(locator);
      connector.create();
      SampleInvocationHandler invocationHandler = new SampleInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      
      // This thread will stop the Connector when NUMBER_OF_CALLS
      // invocations have been received.
      new Thread()
      {
         public void run()
         {
            synchronized (lock)
            {
               while (!done)
               {
                  try
                  {
                     lock.wait();
                  }
                  catch (InterruptedException e)
                  {
                     log.error(e);
                     e.printStackTrace();
                  }
               }
               
               if(connector != null)
               {
                  connector.stop();
                  connector.destroy();
                  log.info("Connector stopped");
                  
                  synchronized(stopLock)
                  {
                     stopLock.notify();
                  }
               }
            }
         }
      }.start();   
   }

   public void tearDown()
   {
   }

   public void setUp() throws Exception
   {
      String locatorURI = getTransport() + "://" + host + ":" + getPort();
      setupServer(locatorURI);
   }

   /**
    * Can pass transport and port to be used as parameters.
    * Valid transports are 'rmi' and 'socket'.
    *
    * @param args
    */
   public static void main(String[] args)
   {
      if(args != null && args.length == 2)
      {
         host = args[0];
         port = Integer.parseInt(args[1]);
      }
      SocketPoolTestServer server = new SocketPoolTestServer();
      try
      {
         server.setUp();
         
         synchronized (stopLock)
         {
            while (!done)
            {
               try
               {
                  stopLock.wait();
                  break;
               }
               catch (InterruptedException ignored) {}
            }
         }
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }

   protected String getTransport()
   {
      return transport;
   }
   
   protected int getPort()
   {
      return port;
   }
   
   /**
    * Simple invocation handler implementation.
    * This is the code that will be called with the invocation payload from the client.
    */
   public static class SampleInvocationHandler implements ServerInvocationHandler
   {
      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         synchronized (lock)
         {
            ++callCounter;
            log.info("callCounter: " + callCounter);
         }
 
         // Waiting for 4 seconds will cause the client to timeout.
         Thread.sleep(4000);
         
         synchronized (lock)
         {
            if (callCounter == NUMBER_OF_CALLS)
            {
               done = true;
               lock.notify();
            }
         }
         return "response";
      }

      public void addListener(InvokerCallbackHandler callbackHandler)
      {
      }
      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
      }
      public void setMBeanServer(MBeanServer server)
      {
      }
      public void setInvoker(ServerInvoker invoker)
      {
      }
   }
}