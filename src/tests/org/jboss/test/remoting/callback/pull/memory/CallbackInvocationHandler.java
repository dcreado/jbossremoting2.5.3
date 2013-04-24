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

package org.jboss.test.remoting.callback.pull.memory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.management.MBeanServer;
import org.jboss.logging.Logger;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.callback.ServerInvokerCallbackHandler;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class CallbackInvocationHandler implements ServerInvocationHandler, Runnable
{
   private transient List pullListeners = new ArrayList();
   private transient List pushListeners = new ArrayList();

   private int numberOfCallbacks = 500;

   private boolean isDone = false;

   private int callbackCounter = 0;

   private byte[] memHolder = null;

   private static final Logger log = Logger.getLogger(CallbackInvocationHandler.class);

   public CallbackInvocationHandler()
   {
      long max = Runtime.getRuntime().maxMemory();
      System.out.println("max mem: " + max);
      log.info("max mem: " + max);
      int memSize = (int) (max * 0.9);
      System.out.println("90% of max: " + memSize);
      log.info("90% of max: " + memSize);
      long free = Runtime.getRuntime().freeMemory();
      System.out.println("free mem: " + free);
      log.info("free mem: " + free);
      long total = Runtime.getRuntime().totalMemory();
      log.info("total mem: " + total);
      if(total != max)
      {
         memHolder = new byte[memSize];
      }
      else if(free > memSize)
      {
         memHolder = new byte[memSize];
      }
   }

   /**
    * called to handle a specific invocation
    *
    * @param invocation
    * @return
    * @throws Throwable
    */
   public Object invoke(InvocationRequest invocation) throws Throwable
   {
      System.out.println("invoke() called on server with param: " + invocation.getParameter());
      log.info("invoke() called on server with param: " + invocation.getParameter());

      if("getdone".equalsIgnoreCase((String) invocation.getParameter()))
      {
         if(isDone)
         {
            return new Boolean(true);
         }
         else
         {
            return new Boolean(false);
         }
      }

      try
      {
         numberOfCallbacks = Integer.parseInt((String) invocation.getParameter());
         System.out.println("Number of callbacks as defined by client = " + numberOfCallbacks);
         log.info("Number of callbacks as defined by client = " + numberOfCallbacks);
      }
      catch(NumberFormatException e)
      {
         System.out.println("Starting callbacks on server.");
         log.info("Starting callbacks on server.");
         new Thread(this).start();
         return "Starting callback";
      }

      return null;
   }

   /**
    * When an object implementing interface <code>Runnable</code> is used to create a thread, starting the thread causes
    * the object's <code>run</code> method to be called in that separately executing thread.
    * <p/>
    * The general contract of the method <code>run</code> is that it may take any action whatsoever.
    *
    * @see Thread#run()
    */
   public void run()
   {

      try
      {
         System.out.println("Sending " + numberOfCallbacks + " callbacks.");
         log.info("Sending " + numberOfCallbacks + " callbacks.");

         synchronized(pullListeners)
         {
            for(int x = 0; x < numberOfCallbacks; x++)
            {
               if(x % 10 == 0)
               {
                  System.out.println("Number of callbacks generated = " + x);
                  log.info("Number of callbacks generated = " + x);
                  System.out.println("Free mem = " + Runtime.getRuntime().freeMemory());
                  log.info("Free mem = " + Runtime.getRuntime().freeMemory());
                  if(isMemLow())
                  {
                     System.out.println("Mem is low, so will be sleeping (slowing test down).");
                     log.info("Mem is low, so will be sleeping (slowing test down).");
                  }
               }
               // Will also fire callback to listeners if they were to exist using
               // simple invocation request.
               synchronized(pullListeners)
               {
                  Iterator itr = pullListeners.iterator();
                  while(itr.hasNext())
                  {
                     InvokerCallbackHandler callbackHandler = (InvokerCallbackHandler) itr.next();
                     try
                     {
                        callbackHandler.handleCallback(new Callback(getCallbackMessage()));
                        if(isMemLow())
                        {
                           Thread.currentThread().sleep(1000);
                        }
                     }
                     catch(HandleCallbackException e)
                     {
                        e.printStackTrace();
                     }
                  }
               }
            }
            // done adding callbacks, now release memory
            memHolder = null;
         }

         isDone = true;

         synchronized(pushListeners)
         {
            Iterator itr = pushListeners.iterator();
            while(itr.hasNext())
            {
               InvokerCallbackHandler handler = (InvokerCallbackHandler) itr.next();
               try
               {
                  handler.handleCallback(new Callback("Done"));
               }
               catch(HandleCallbackException e)
               {
                  e.printStackTrace();
               }
            }
         }
      }
      catch(Throwable e)
      {
         e.printStackTrace();
      }

   }

   private boolean isMemLow()
   {
      Runtime runtime = Runtime.getRuntime();
      long max = runtime.maxMemory();
      long total = runtime.totalMemory();
      long free = runtime.freeMemory();
      float percentage = 100 * free / total;
      if(max == total && 40 >= percentage)
      {
         return true;
      }
      else
      {
         return false;
      }
   }


   /**
    * Adds a callback handler that will listen for callbacks from the server invoker handler.
    *
    * @param callbackHandler
    */
   public void addListener(InvokerCallbackHandler callbackHandler)
   {
      ServerInvokerCallbackHandler sih = (ServerInvokerCallbackHandler) callbackHandler;

      if(!sih.isPullCallbackHandler())
      {
         pushListeners.add(callbackHandler);
      }
      else
      {
         pullListeners.add(callbackHandler);
      }
   }

   /**
    * Removes the callback handler that was listening for callbacks from the server invoker handler.
    *
    * @param callbackHandler
    */
   public void removeListener(InvokerCallbackHandler callbackHandler)
   {
      pullListeners.remove(callbackHandler);
      pushListeners.remove(callbackHandler);
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

   private Object getCallbackMessage()
   {
      callbackCounter++;
      //byte[] bytes = new byte[5120000];
      byte[] bytes = new byte[102400];
      TestCallback callback = new TestCallback(bytes, callbackCounter);
      return callback;
   }


}