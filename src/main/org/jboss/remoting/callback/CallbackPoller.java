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
package org.jboss.remoting.callback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.transport.ClientInvoker;

/**
 * CallbackPoller is used to simulate push callbacks on transports that don't support
 * bidirectional connections.  It will periodically pull callbacks from the server
 * and pass them to the InvokerCallbackHandler.
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 */
public class CallbackPoller extends TimerTask implements Runnable
{
   /*
    * Implementation note.
    *
    * CallbackPoller uses two, or possibly three, threads.  The first thread is the
    * Timer thread, which periodically pulls callbacks from the server and adds them
    * to toHandleList.  The second thread takes callbacks from toHandleList, passes
    * them to the CallbackHandler, and, if an acknowledgement is requested for a
    * callback, it adds the callback to toAcknowledgeList.  The third thread, which is
    * created in response to the first callback for which an acknowledgement is requested,
    * takes the contents of toAcknowledgeList and acknowledges them in a batch.
    *
    * CallbackPoller will not shut down until all received callbacks have been processed
    * by the CallbackHandler and acknowledgements have been sent for all callbacks for
    * which acknowledgements have been requested.
    */

   /**
    * Default polling period for getting callbacks from the server.
    * Default is 5000 milliseconds.
    */
   public static final long DEFAULT_POLL_PERIOD = 5000;
   
   /**
    * Default timeout for getting callbacks in blocking mode.
    * Default is 5000 milliseconds.
    */
   public static final int DEFAULT_BLOCKING_TIMEOUT = 5000;
   
   /**
    * Default number of exceptions before callback polling wil be shut down.
    * Default is 5.
    */
   public static final int DEFAULT_MAX_ERROR_COUNT = 5;
   
   /**
    * The key value to use to specify if stop() should wait for the call to
    * org.jboss.remoting.Client.getCallbacks() should return.  The default 
    * behavior is do a synchronized shutdown for nonblocking callbacks and
    * a nonsynchronized shutdown for blocking callbacks.
    */
   public static final String SYNCHRONIZED_SHUTDOWN = "doSynchronizedShutdown";
   
   /**
    * The key value to use to specify the desired poll period
    * within the metadata Map.
    */
   public static final String CALLBACK_POLL_PERIOD = "callbackPollPeriod";

   /** Use java.util.timer.schedule(). */
   public static final String SCHEDULE_FIXED_RATE = "scheduleFixedRate";

   /** Use java.util.timer.scheduleAtFixedRate(). */
   public static final String SCHEDULE_FIXED_DELAY = "scheduleFixedDelay";
   
   /**
    * The key to use to specify the number of errors before callback polling
    * will be shut down.
    */
   public static final String MAX_ERROR_COUNT = "maxErrorCount";

   /** The key to use in metadata Map to request statistics.  The associated
    *  is ignored. */
   public static final String REPORT_STATISTICS = "reportStatistics";

   private Client client = null;
   private InvokerCallbackHandler callbackHandler = null;
   private Map metadata = null;
   private Object callbackHandlerObject = null;
   private boolean blocking = false;
   private boolean synchronizedShutdown = false;
   private long pollPeriod = DEFAULT_POLL_PERIOD;
   private Timer timer;
   private String scheduleMode = SCHEDULE_FIXED_RATE;
   private boolean reportStatistics;
   private boolean running;
   private int maxErrorCount = -1;
   private int errorCount;
   private boolean useAllParams;

   private ArrayList toHandleList = new ArrayList();
   private ArrayList toAcknowledgeList = new ArrayList();
   private HandleThread handleThread;
   private AcknowledgeThread acknowledgeThread;
   private BlockingPollerThread blockingPollerThread;

   private static final Logger log = Logger.getLogger(CallbackPoller.class);


   public CallbackPoller(Client client, InvokerCallbackHandler callbackhandler, Map metadata, Object callbackHandlerObject)
   {
      this.client = client;
      this.callbackHandler = callbackhandler;
      this.metadata = new HashMap(metadata);
      this.callbackHandlerObject = callbackHandlerObject;
   }

   public void start() throws Exception
   {
      if (callbackHandler == null)
      {
         throw new NullPointerException("Can not poll for callbacks when InvokerCallbackHandler is null.");
      }
      if (client != null)
      {
         client.connect();
      }
      else
      {
         throw new NullPointerException("Can not poll for callbacks when Client is null.");
      }

      configureParameters();

      handleThread = new HandleThread("HandleThread");
      handleThread.start();
      if (log.isTraceEnabled()) log.trace("blocking: " + blocking);
      if (blocking)
      {
         if (maxErrorCount == -1)
            maxErrorCount = DEFAULT_MAX_ERROR_COUNT;
         
         running = true;
         metadata.put(Client.THROW_CALLBACK_EXCEPTION, "true");
         blockingPollerThread = new BlockingPollerThread();
         blockingPollerThread.start();
      }
      else
      {
         timer = new Timer(true);
         if (SCHEDULE_FIXED_DELAY.equals(scheduleMode))
            timer.schedule(this, pollPeriod, pollPeriod);
         else
            timer.scheduleAtFixedRate(this, pollPeriod, pollPeriod);
      }
   }

   public synchronized void run()
   {
      // need to pull callbacks from server and give them to callback handler
      try
      {
         if (log.isTraceEnabled()) log.trace(this + " getting callbacks for " + callbackHandler);
         List callbacks = client.getCallbacks(callbackHandler, metadata);
         if (log.isTraceEnabled()) log.trace(this + " callback count: " + (callbacks == null ? 0 : callbacks.size()));

         if (callbacks != null && callbacks.size() > 0)
         {
            synchronized (toHandleList)
            {
               toHandleList.addAll(callbacks);
               if (toHandleList.size() == callbacks.size())
                  toHandleList.notify();
            }
         }

         if (reportStatistics)
            reportStatistics(callbacks);
      }
      catch (Throwable throwable)
      {
    	 if (!running)
         {
            stop();
            return;
         }
         
         log.info(this + " Error getting callbacks from server.");
         log.debug(this + " Error getting callbacks from server.", throwable);
         String errorMessage = throwable.getMessage();
         if (errorMessage != null)
         {
            if (errorMessage.startsWith("Could not find listener id"))
            {
               log.error("Client no longer has InvokerCallbackHandler (" + 
                          callbackHandler +
                         ") registered.  Shutting down callback polling");
               stop();
               return;
            }
            if (errorMessage.startsWith("Can not make remoting client invocation " +
                                        "due to not being connected to server."))
            {
               log.error("Client no longer connected.  Shutting down callback polling");
               stop();
               return;
            }
         }
         if (maxErrorCount >= 0)
         {
            if (++errorCount > maxErrorCount)
            {
               log.error("Error limit of " + maxErrorCount + 
                          " exceeded.  Shutting down callback polling");
               stop();
               return;
            }
         }
      }
   }
   
   public void stop()
   {
      stop(-1);
   }

   /**
    * stop() will not return until all received callbacks have been processed
    * by the CallbackHandler and acknowledgements have been sent for all callbacks for
    * which acknowledgements have been requested.
    */
   public void stop(int timeout)
   {
      log.debug(this + " is shutting down");
      running = false;
      
      if (!blocking)
      {
         cancel();
         
         if (timer != null)
         {
            timer.cancel();
            timer = null;
         }
      }
      
      if (timeout == 0)
         return;
      
      if (synchronizedShutdown)
      {
         // run() and stop() are synchronized so that stop() will wait until run() has finished
         // adding any callbacks it has received to toHandleList.  Therefore, once cancel()
         // returns, no more callbacks will arrive from the server.
         synchronized (this)
         {
            shutdown();
         }
      }
      else
      {
         shutdown();
      }

      log.debug(this + " has shut down");
   }

   
   private void shutdown()
   {
      // HandleThread.shutdown() will not return until all received callbacks have been
      // processed and, if necessary, added to toAcknowledgeList.
      if (handleThread != null)
      {
         handleThread.shutdown();
         handleThread = null;
      }

      // AcknowledgeThread.shutdown() will not return until acknowledgements have been sent
      // for all callbacks for which acknowledgements have been requested.
      if (acknowledgeThread != null)
      {
         acknowledgeThread.shutdown();
         acknowledgeThread = null;
      }
   }
   
   
   class BlockingPollerThread extends Thread
   {
      public BlockingPollerThread()
      {
         String threadName = getName();
         int i = threadName.indexOf('-');
         String threadNumber = null;
         if (i >= 0)
            threadNumber = threadName.substring(i+1);
         else
            threadNumber = Long.toString(System.currentTimeMillis());
         String pollerString = CallbackPoller.this.toString();
         String address = pollerString.substring(pollerString.indexOf('@'));
         setName("CallbackPoller:" + threadNumber + "[" + address + "]");
         setDaemon(true);
      }

      public void run()
      {
         while (running)
         {
            CallbackPoller.this.run();
         }
      }
   }  

   
   class HandleThread extends Thread
   {
      boolean running = true;
      boolean done;
      ArrayList toHandleListCopy = new ArrayList();
      Callback callback;

      HandleThread(String name)
      {
         super(name);
      }
      public void run()
      {
         while (true)
         {
            synchronized (toHandleList)
            {
               if (toHandleList.isEmpty() && running)
               {
                  try
                  {
                     toHandleList.wait();
                  }
                  catch (InterruptedException e)
                  {
                     log.debug("unexpected interrupt");
                     continue;
                  }
               }

               // If toHandleList is empty, then running must be false.  We return
               // only when both conditions are true.
               if (toHandleList.isEmpty())
               {
                  done = true;
                  toHandleList.notify();
                  return;
               }

               toHandleListCopy.addAll(toHandleList);
               toHandleList.clear();
            }

            while (!toHandleListCopy.isEmpty())
            {
               try
               {
                  callback = (Callback) toHandleListCopy.remove(0);
                  callback.setCallbackHandleObject(callbackHandlerObject);
                  callbackHandler.handleCallback(callback);
               }
               catch (HandleCallbackException e)
               {
                  log.error("Error delivering callback to callback handler (" + callbackHandler + ").", e);
               }

               checkForAcknowledgeRequest(callback);
            }
         }
      }

      /**
       *  Once CallbackPoller.stop() has called HandleThread.shutdown(), CallbackPoller.run()
       *  has terminated and no additional callbacks will be received.  shutdown() will
       *  not return until HandleThread has processed all received callbacks.
       *
       *  Either run() or shutdown() will enter its own synchronized block first.
       *
       *  case 1): run() enters its synchronized block first:
       *     If toHandleList is empty, then run() will reach toHandleList.wait(), shutdown()
       *     will wake up run(), and run() will exit.  If toHandleList is not empty, then run()
       *     will process all outstanding callbacks and return to its synchronized block.  At
       *     this point, either case 1) (with toHandleList empty) or case 2) applies.
       *
       *  case 2): shutdown() enters its synchronized block first:
       *     run() will process all outstanding callbacks and return to its synchronized block.
       *     After shutdown() reaches toHandleList.wait(), run() will enter its synchronized
       *     block, find running == false and toHandleList empty, and it will exit.
       */
      protected void shutdown()
      {
         log.debug(this + " is shutting down");
         synchronized (toHandleList)
         {
            running = false;
            toHandleList.notify();
            while (!done)
            {
               try
               {
                  toHandleList.wait();
               }
               catch (InterruptedException ignored) {}
            }
         }
         log.debug(this + " has shut down");
         return;
      }
   }


   class AcknowledgeThread extends Thread
   {
      boolean running = true;
      boolean done;
      ArrayList toAcknowledgeListCopy = new ArrayList();

      AcknowledgeThread(String name)
      {
         super(name);
      }
      public void run()
      {
         while (true)
         {
            synchronized (toAcknowledgeList)
            {
               while (toAcknowledgeList.isEmpty() && running)
               {
                  try
                  {
                     toAcknowledgeList.wait();
                  }
                  catch (InterruptedException e)
                  {
                     log.debug("unexpected interrupt");
                     continue;
                  }
               }

               // If toAcknowledgeList is empty, then running must be false.  We return
               // only when both conditions are true.
               if (toAcknowledgeList.isEmpty())
               {
                  done = true;
                  toAcknowledgeList.notify();
                  return;
               }

               toAcknowledgeListCopy.addAll(toAcknowledgeList);
               toAcknowledgeList.clear();
            }

            try
            {
               if (log.isTraceEnabled())
               {
                  Iterator it = toAcknowledgeListCopy.iterator();
                  while (it.hasNext())
                  {
                     Callback cb = (Callback) it.next();
                     Map map = cb.getReturnPayload();
                     log.trace("acknowledging: " + map.get(ServerInvokerCallbackHandler.CALLBACK_ID));
                  }
               }
               client.acknowledgeCallbacks(callbackHandler, toAcknowledgeListCopy);
               toAcknowledgeListCopy.clear();
            }
            catch (Throwable t)
            {
               log.error("Error acknowledging callback for callback handler (" + callbackHandler + ").", t);
            }
         }
      }

      /**
       *  Once CallbackPoller.stop() has called AcknowledgeThread.shutdown(), HandleThread
       *  has terminated and no additional callbacks will be added to toAcknowledgeList.
       *  shutdown() will not return until AcknowledgeThread has acknowledged all callbacks
       *  in toAcknowledgeList.
       *
       *  Either run() or shutdown() will enter its own synchronized block first.
       *
       *  case 1): run() enters its synchronized block first:
       *     If toAcknowledgeList is empty, then run() will reach toAcknowledgeList.wait(),
       *     shutdown() will wake up run(), and run() will exit.  If toAcknowledgeList is not
       *     empty, then run() will process all callbacks in toAcknowledgeList and return to
       *     its synchronized block.  At this point, either case 1) (with toAcknowledgeList
       *     empty) or case 2) applies.
       *
       *  case 2): shutdown() enters its synchronized block first:
       *     run() will process all callbacks in toAcknowledgeList and return to its
       *     synchronized block.  After shutdown() reaches toAcknowledgeList.wait(), run()
       *     will enter its synchronized block, find running == false and toAcknowledgeList
       *     empty, and it will exit.
       */
      public void shutdown()
      {
         log.debug(this + " is shutting down");      
         synchronized (toAcknowledgeList)
         {
            running = false;
            toAcknowledgeList.notify();
            while (!done)
            {
               try
               {
                  toAcknowledgeList.wait();
               }
               catch (InterruptedException ignored) {}
            }
         }
         log.debug(this + " has shut down");
         return;
      }
   }


   private void checkForAcknowledgeRequest(Callback callback)
   {
      Map returnPayload = callback.getReturnPayload();
      if (returnPayload != null)
      {
         Object callbackId = returnPayload.get(ServerInvokerCallbackHandler.CALLBACK_ID);
         if (callbackId != null)
         {
            Object o = returnPayload.get(ServerInvokerCallbackHandler.REMOTING_ACKNOWLEDGES_PUSH_CALLBACKS);
            if (o instanceof String  && Boolean.valueOf((String)o).booleanValue() ||
                o instanceof Boolean && ((Boolean)o).booleanValue())
            {
               synchronized (toAcknowledgeList)
               {
                  toAcknowledgeList.add(callback);
                  if (toAcknowledgeList.size() == 1)
                  {
                     if (acknowledgeThread == null)
                     {
                        acknowledgeThread = new AcknowledgeThread("AcknowledgeThread");
                        acknowledgeThread.start();
                     }
                     else
                     {
                        toAcknowledgeList.notify();
                     }
                  }
               }
            }
         }
      }
   }
   
   
   private void configureParameters()
   {
      Map config = new HashMap();
      ClientInvoker invoker = client.getInvoker();
      if (invoker != null)
      {
         config.putAll(invoker.getLocator().getParameters());
      }
      config.putAll(client.getConfiguration());
      config.putAll(metadata);
      
      Object val = config.get(Client.USE_ALL_PARAMS);
      if (val != null)
      {
         if (val instanceof String)
         {
            useAllParams = Boolean.valueOf((String) val).booleanValue();
         }
         else
         {
            log.warn("Value for " + Client.USE_ALL_PARAMS + " must be of type " +
                     String.class.getName() + " and is " + val.getClass().getName());
         }
      }
      log.debug(this + ": useAllParams: " + useAllParams);
      if (!useAllParams)
      {
         config = metadata;
      }
      
      val = config.get(ServerInvoker.BLOCKING_MODE);
      if (val != null)
      {
         if (val instanceof String)
         {
            if (ServerInvoker.BLOCKING.equals(val))
            {
               blocking = true;
               synchronizedShutdown = false;
            }
            else if (ServerInvoker.NONBLOCKING.equals(val))
            {
               blocking = false;
               synchronizedShutdown = true;
            }
            else
            {
               log.warn("Value for " + ServerInvoker.BLOCKING_MODE + 
                     " configuration is " + val + ". Must be either " +
                     ServerInvoker.BLOCKING + " or " + ServerInvoker.NONBLOCKING +
                     ". Using " + ServerInvoker.BLOCKING + ".");
            }
         }
         else
         {
            log.warn("Value for " + ServerInvoker.BLOCKING_MODE + 
                  " configuration must be of type " + String.class.getName() +
                  " and is of type " + val.getClass().getName());
         }
      }

      // Default blocking mode on server is nonblocking.
      if (blocking)
         metadata.put(ServerInvoker.BLOCKING_MODE, ServerInvoker.BLOCKING);

      val = config.get(ServerInvoker.BLOCKING_TIMEOUT);
      if (val != null)
      {
         if (val instanceof String)
         {
            try
            {
               int blockingTimeout = Integer.parseInt((String) val);
               metadata.put(ServerInvoker.TIMEOUT, Integer.toString(blockingTimeout));
            }
            catch (NumberFormatException e)
            {
               log.warn("Error converting " + ServerInvoker.BLOCKING_TIMEOUT + " to type long.  " + e.getMessage());
            }
         }
         else
         {
            log.warn("Value for " + ServerInvoker.BLOCKING_TIMEOUT + " configuration must be of type " + String.class.getName() +
                  " and is " + val.getClass().getName());
         }
      }

      val = config.get(SYNCHRONIZED_SHUTDOWN);
      if (val != null)
      {
         if (val instanceof String)
         {
            synchronizedShutdown = Boolean.valueOf((String) val).booleanValue();
         }
         else
         {
            log.warn("Value for " + SYNCHRONIZED_SHUTDOWN + " must be of type " + String.class.getName() +
                  " and is " + val.getClass().getName());
         }
      }

      val = config.get(CALLBACK_POLL_PERIOD);
      if (val != null)
      {
         if (val instanceof String)
         {
            try
            {
               pollPeriod = Long.parseLong((String) val);
            }
            catch (NumberFormatException e)
            {
               log.warn("Error converting " + CALLBACK_POLL_PERIOD + " to type long.  " + e.getMessage());
            }
         }
         else
         {
            log.warn("Value for " + CALLBACK_POLL_PERIOD + " configuration must be of type " + String.class.getName() +
                  " and is " + val.getClass().getName());
         }
      }
      val = config.get(CALLBACK_SCHEDULE_MODE);
      if (val != null)
      {
         if (val instanceof String)
         {
            if (SCHEDULE_FIXED_DELAY.equals(val) || SCHEDULE_FIXED_RATE.equals(val))
            {
               scheduleMode = (String) val;
            }
            else
            {
               log.warn("Unrecognized value for " + CALLBACK_SCHEDULE_MODE + ": " + val);
               log.warn("Using " + scheduleMode);
            }
         }
         else
         {
            log.warn("Value for " + CALLBACK_SCHEDULE_MODE + " must be of type " + String.class.getName() +
                  " and is " + val.getClass().getName());
         }
      }
      val = config.get(MAX_ERROR_COUNT);
      if (val != null)
      {
         if (val instanceof String)
         {
            try
            {
               maxErrorCount = Integer.parseInt((String) val);
            }
            catch (NumberFormatException e)
            {
               log.warn("Error converting " + MAX_ERROR_COUNT + " to type int.  " + e.getMessage());
            }
         }
         else
         {
            log.warn("Value for " + MAX_ERROR_COUNT + " configuration must be of type " + String.class.getName() +
                  " and is " + val.getClass().getName());
         }
      }
      if (config.get(REPORT_STATISTICS) != null)
      {
         reportStatistics = true;
      }
   }


   private void reportStatistics(List callbacks)
   {
      int toHandle;
      int toAcknowledge = 0;

      synchronized (toHandleList)
      {
         toHandle = toHandleList.size() + handleThread.toHandleListCopy.size();
      }

      synchronized (toAcknowledgeList)
      {
         if (acknowledgeThread != null)
            toAcknowledge = toAcknowledgeList.size() + acknowledgeThread.toAcknowledgeListCopy.size();
      }

      StringBuffer message = new StringBuffer("\n");
      message.append("================================\n")
             .append("  retrieved " + callbacks.size() + " callbacks\n")
             .append("  callbacks waiting to be processed: " + toHandle + "\n")
             .append("  callbacks waiting to be acknowledged: " + toAcknowledge + "\n")
             .append("================================");
      log.info(message);
   }


   /**
    * The key value to use in metadata Map to specify the desired scheduling mode. 
    */
   public static final String CALLBACK_SCHEDULE_MODE = "scheduleMode";
}