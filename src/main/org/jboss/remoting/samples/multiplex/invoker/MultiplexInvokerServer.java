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

/*
 * Created on Oct 4, 2005
 */
 
package org.jboss.remoting.samples.multiplex.invoker;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.management.MBeanServer;

import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;

/**
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 */
public class MultiplexInvokerServer
{
   private SampleInvocationHandler handler;
   private Connector connector = null;

   
   public void init() throws Exception
   {
      connector = new Connector();
      InvokerLocator locator = new InvokerLocator("multiplex://localhost:9090");
      connector.setInvokerLocator(locator.getLocatorURI());
      connector.create();
      handler = new SampleInvocationHandler();
      connector.addInvocationHandler("sample", handler);
      connector.start();
      System.out.println("Started server at: " + connector.getInvokerLocator());
   }

   
   public boolean isDone()
   {
      return handler.isDone();
   }

   
   protected void setUp() throws Exception
   {
      init();
   }


   protected void tearDown() throws Exception
   {
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }
   

   public static void main(String[] args)
   {
      MultiplexInvokerServer test = new MultiplexInvokerServer();
      try
      {
         test.setUp();
         
         while (!test.isDone())
            Thread.sleep(1000);
            
         test.tearDown();
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }
   
 
   /**
    * Simple invocation handler implementation.  When callback client's are registered, will
    * generate callbacks periodically.
    */
   public static class SampleInvocationHandler implements ServerInvocationHandler, Runnable
   {
      // list of callback listeners registered
      private List listeners = new ArrayList();
      private boolean didCallbacks = false;
      
      public SampleInvocationHandler()
      {
         // will start a new thread for generating callbacks.
         Thread callbackThread = new Thread(this);
         callbackThread.setDaemon(true);
         callbackThread.start();
      }
      
      /**
       * called by the remoting server to handle the invocation from client.
       *
       * @param invocation
       * @return
       * @throws Throwable
       */
      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         // Print out the invocation request
         System.out.println("Invocation request is: " + invocation.getParameter());
         
         // Return invocation parameter
         return invocation.getParameter();
      }
      
      /**
       * Adds a callback handler that will listen for callbacks from
       * the server invoker handler.
       *
       * @param callbackHandler
       */
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         System.out.println("Adding callback listener.");
         listeners.add(callbackHandler);
      }
      
      /**
       * Removes the callback handler that was listening for callbacks
       * from the server invoker handler.
       *
       * @param callbackHandler
       */
      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         System.out.println("Removing callback listener.");
         listeners.remove(callbackHandler);
      }
      
      /**
       * Will generate callback messages every second while shouldGenerateCallbacks
       * flag is true.
       */
      public void run()
      {
         // wait for a callback listener to be registered
         while (listeners.isEmpty())
         {
            try
            {
               Thread.sleep(1000);
            }
            catch(InterruptedException e)
            {
            }
         }
         
         for (int i = 0; i < 2; i++)
         {
            // create new callback message
            Callback callback = new Callback(new Integer(29 * (i + 1)));
            System.out.println("generating callback value: " + callback.getCallbackObject());
            
            // get a copy of the listener list to avoid ConcurrentModificationException
            List localListeners= new ArrayList(listeners);
            
            // send callback to all registered listeners
            Iterator itr = localListeners.iterator();
            while(itr.hasNext())
            {
               InvokerCallbackHandler callbackHandler = (InvokerCallbackHandler) itr.next();
               
               try
               {
                  callbackHandler.handleCallback(callback);
               }
               catch(HandleCallbackException e)
               {
                  e.printStackTrace();
               }
               
               try
               {
                  Thread.sleep(2000);
               }
               catch (InterruptedException ignored)
               {
               }
            } 
         }
         
         didCallbacks = true;
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
      
      
      public boolean isDone()
      {
         return didCallbacks;
      }
   }
}

