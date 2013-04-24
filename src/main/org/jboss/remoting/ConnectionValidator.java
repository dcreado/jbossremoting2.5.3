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

package org.jboss.remoting;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.jboss.logging.Logger;
import org.jboss.remoting.transport.ClientInvoker;
import org.jboss.remoting.util.StoppableTimerTask;
import org.jboss.remoting.util.TimerUtil;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @author <a href="mailto:tlee@redhat.com">Trustin Lee</a>
 */
public class ConnectionValidator extends TimerTask implements StoppableTimerTask
{
   // Constants ------------------------------------------------------------------------------------

   private static final Logger log = Logger.getLogger(ConnectionValidator.class.getName());

   /** Configuration map key for ping period. */
   public static final String VALIDATOR_PING_PERIOD = "validatorPingPeriod";
   
   /** Default ping period. Value is 2 seconds. */
   public static final long DEFAULT_PING_PERIOD = 2000;
   
   /** Configuration map key for ping timeout. */
   public static final String VALIDATOR_PING_TIMEOUT = "validatorPingTimeout";
   
   /** Default ping timeout period.  Value is 1 second. */
   public static final String DEFAULT_PING_TIMEOUT = "1000";

   /** Default ping timeout period.  Value is 1 second. */
   public static final int DEFAULT_PING_TIMEOUT_INT = 1000;
   
   /**
    * Default number of ping retries.  Value is 1.
    * Currently implemented only on socket transport family.
    */
   public static final String DEFAULT_NUMBER_OF_PING_RETRIES = "1";

   /**
    * Key to determine if ConnectionValidator should tie failure to presence
    * of active lease on server side.  Default value is "true".
    */
   public static final String TIE_TO_LEASE = "tieToLease";
   
   /**
    * Key to determine whether to stop ConnectionValidator when PING fails.
    * Default value is "true".
    */
   public static final String STOP_LEASE_ON_FAILURE = "stopLeaseOnFailure";
   
   /**
    * Key to determine value of disconnectTimeout upon connection failure.
    */
   public static final String FAILURE_DISCONNECT_TIMEOUT = "failureDisconnectTimeout";
   
   // Static ---------------------------------------------------------------------------------------

   private static boolean trace = log.isTraceEnabled();

   /**
    * Will make $PING$ invocation on server. If sucessful, will return true. Otherwise, will throw
    * an exception.
    *
    * @param locator - locator for the server to ping
    * @param config  - any configuration needed for server
    * @return true if alive, false if not
    */
   public static boolean checkConnection(final InvokerLocator locator, Map config) throws Throwable
   {
      boolean pingWorked = false;
      final Map configMap = createPingConfig(config, null);
      int pingTimeout = Integer.parseInt((String) configMap.get(ServerInvoker.TIMEOUT));
      ClientInvoker innerClientInvoker = null;

      try
      {  
         try
         {
            innerClientInvoker = (ClientInvoker) AccessController.doPrivileged( new PrivilegedExceptionAction()
            {
               public Object run() throws Exception
               {
                  return InvokerRegistry.createClientInvoker(locator, configMap);
               }
            });
         }
         catch (PrivilegedActionException pae)
         {
            throw pae.getException();
         }

         if (!innerClientInvoker.isConnected())
         {
            if (trace) { log.trace("inner client invoker not connected, connecting ..."); }
            innerClientInvoker.connect();
         }

         pingWorked = doCheckConnection(innerClientInvoker, pingTimeout);
      }
      catch (Throwable throwable)
      {
         log.debug("ConnectionValidator unable to connect to server " +
            innerClientInvoker.getLocator().getProtocol() + "://" +
            innerClientInvoker.getLocator().getHost() + ":" +
            innerClientInvoker.getLocator().getPort(), throwable);
      }
      finally
      {
         if (innerClientInvoker != null)
         {
            AccessController.doPrivileged( new PrivilegedAction()
            {
               public Object run()
               {
                  InvokerRegistry.destroyClientInvoker(locator, configMap);
                  return null;
               }
            });
         }
      }

      return pingWorked;
   }

   private static boolean doCheckConnection(ClientInvoker clientInvoker, int pingTimeout) throws Throwable
   {
      boolean pingWorked = false;

      try
      {
         // Sending null client id as don't want to trigger lease on server side. This also means
         // that client connection validator will NOT impact client lease, so can not depend on it
         // to maintain client lease with the server.
         InvocationRequest ir;
         ir = new InvocationRequest(null, Subsystem.SELF, "$PING$", null, null, null);
         ConnectionCheckThread t = new ConnectionCheckThread(clientInvoker, ir);
         t.start();
         Thread.sleep(pingTimeout);
         pingWorked = t.isValid();
      }
      catch (Throwable t)
      {
         log.debug("ConnectionValidator failed to ping via " + clientInvoker, t);
      }

      return pingWorked;
   }
   
   private static Map createPingConfig(Map config, Map metadata)
   {
      Map localConfig = new HashMap();
      localConfig.put("connection_checker", "true");

      if (config != null)
      {
         Object o = config.get(VALIDATOR_PING_TIMEOUT);
         log.trace("config timeout: " + o);
         if (o != null)
         {
            try
            {
               Integer.parseInt((String) o);
               localConfig.put(ServerInvoker.TIMEOUT, o);
            }
            catch (NumberFormatException e)
            {
               log.warn("Need integer for value of parameter " + VALIDATOR_PING_TIMEOUT + 
                        ". Using default value " + DEFAULT_PING_TIMEOUT);
            }
         }
         
         o = config.get("NumberOfCallRetries");
         if (o != null)
         {
            localConfig.put("NumberOfCallRetries", o);
         }
      }
      
      if (metadata != null)
      {
         metadata.remove(ServerInvoker.TIMEOUT);
         localConfig.putAll(metadata);
         Object o = metadata.get(VALIDATOR_PING_TIMEOUT);
         if (o != null)
         {
            try
            {
               Integer.parseInt((String) o);
               localConfig.put(ServerInvoker.TIMEOUT, o);
            }
            catch (NumberFormatException e)
            {
               log.warn("Need integer for value of parameter " + VALIDATOR_PING_TIMEOUT +
                        ". Using default value " + DEFAULT_PING_TIMEOUT);
            }
         }
      }
      
      if (localConfig.get(ServerInvoker.TIMEOUT) == null)
      {
         localConfig.put(ServerInvoker.TIMEOUT, DEFAULT_PING_TIMEOUT);
      }
      
      if (localConfig.get("NumberOfCallRetries") == null)
      {
         localConfig.put("NumberOfCallRetries", DEFAULT_NUMBER_OF_PING_RETRIES);
      }
      
      return localConfig;
   }

   // Attributes -----------------------------------------------------------------------------------

   private Client client;
   private long pingPeriod;
   private Map metadata;
   private InvokerLocator locator;
   private Map configMap;
   private Map listeners;
   private ClientInvoker clientInvoker;
   private Object lock = new Object();
   private Object notificationLock = new Object();
   private boolean started;
   private volatile boolean stopped;
   private volatile boolean stopping;
   private String invokerSessionId;
   private boolean tieToLease = true;
   private boolean stopLeaseOnFailure = true;
   private int pingTimeout;
   private int failureDisconnectTimeout = -1;
   private volatile boolean isValid;
   private Timer timer;
   private MicroRemoteClientInvoker sharedInvoker;
   private LeasePinger leasePinger;
   private boolean useClientConnectionIdentity;

   // Constructors ---------------------------------------------------------------------------------

   public ConnectionValidator(Client client)
   {
      this(client, DEFAULT_PING_PERIOD);
   }

   public ConnectionValidator(Client client, long pingPeriod)
   {
      this.client = client;
      this.locator = client.getInvoker().getLocator();
      this.pingPeriod = pingPeriod;
      pingTimeout = DEFAULT_PING_TIMEOUT_INT;
      listeners = new HashMap();
      stopped = false;
      getParameters(client, new HashMap());
      log.debug(this + " created");
   }
   
   public ConnectionValidator(Client client, Map metadata)
   {
      this.client = client;
      this.locator = client.getInvoker().getLocator();
      pingPeriod = DEFAULT_PING_PERIOD;
      pingTimeout = DEFAULT_PING_TIMEOUT_INT;
      listeners = new HashMap();
      stopped = false;
      this.metadata = new HashMap(metadata);
      getParameters(client, metadata);
      log.debug(this + " created");
   }

   // StoppableTimerTask implementation ------------------------------------------------------------

   public void stop()
   {
      if (stopped)
      {
         return;
      }

      doStop();
   }

   // TimerTask overrides --------------------------------------------------------------------------

   /**
    * The action to be performed by this timer task.
    */
   public void run()
   {
      synchronized (lock)
      {
         if (!started)
         {
            throw new IllegalStateException(
                  ConnectionValidator.class.getName() + ".run() should not be " +
                  "called directly; use " + ConnectionValidator.class.getName() +
                  ".addConnectionListener() instead.");
         }
         
         if (stopping)
         {
            return;
         }
         
         TimerTask tt = new WaitOnConnectionCheckTimerTask();

         try
         {
            timer.schedule(tt, 0);
         }
         catch (IllegalStateException e)
         {
            log.debug("Unable to schedule TimerTask on existing Timer", e);
            timer = new Timer(true);
            timer.schedule(tt, 0);
         }
      }

      try
      {
         if(!stopping)
         {
            isValid = false;

            if (tieToLease && client.getLeasePeriod() > 0)
            {
               if (trace)
               {
                  log.trace(this + " sending PING tied to lease");
               }
               isValid = doCheckConnectionWithLease();
            }
            else
            {
               if (trace) { log.trace(this + " pinging ..."); }
               isValid = doCheckConnectionWithoutLease();
            }
         }
      }
      catch (Throwable thr)
      {
         log.debug(this + " got throwable while pinging", thr);

         if (stopLeaseOnFailure)
         {
            log.debug(this + " detected connection failure: stopping");
            cancel();
         }
      }
      finally
      {
         synchronized (notificationLock)
         {
            notificationLock.notifyAll();
         }
      }
   }

   public boolean cancel()
   {
      return doStop();
   }

   // Public ---------------------------------------------------------------------------------------

   public boolean addConnectionListener(Client client, ConnectionListener listener)
   {
      boolean doStart = false;
      if (listener != null)
      {
         synchronized (lock)
         {
            if (stopping)
            {
               if (trace) log.trace(this + " is stopped. Cannot add ConnectionListener: " + listener + " for " + client);
               return false;
            }
            if (listeners.size() == 0)
            {
               doStart = true;
            }
            Set s = (Set) listeners.get(listener);
            if (s == null)
            {
               s = new HashSet();
            }
            s.add(client);
            listeners.put(listener, s);
            log.debug(this + " added ConnectionListener: " + listener + " for " + client);
         }
         if (doStart)
         {
            start();
         }
      }
      
      return true;
   }

   public boolean removeConnectionListener(Client client, ConnectionListener listener)
   {
      if (listener == null)
      {
         if (trace) log.trace(this + " ConnectionListener is null");
         return false;
      }
      synchronized (lock)
      {
         if (stopping)
         {
            if (trace) log.trace(this + " is stopped. It's too late to remove " + listener);
            return false;
         }
         Set s = (Set) listeners.get(listener);
         if (s == null)
         {
            log.debug(this + ": " + listener + " is not registered");
            return false;
         }
         if (s.remove(client))
         {
            log.debug(this + " removed ConnectionListener: " + listener + " for " + client);
         }
         else
         {
            log.debug(this + ": " + listener + " is not registered for " + client);
            return false;
         }
         if (s.size() == 0)
         {
            listeners.remove(listener);
         }
         if (listeners.size() == 0)
         {
            stop();
         }
      }
      return true;
   }

   public long getPingPeriod()
   {
      if (stopping)
      {
         return -1;
      }

      return pingPeriod;
   }

   public String toString()
   {
      return "ConnectionValidator[" + Integer.toHexString(System.identityHashCode(this)) + ":" + clientInvoker + ", pingPeriod=" + pingPeriod + " ms]";
   }
   
   public boolean isStopped()
   {
      return stopped;
   }

   // Package protected ----------------------------------------------------------------------------

   void notifyListeners(Throwable thr)
   {
      final Throwable t = thr;
      synchronized (lock)
      {
         if (stopping)
         {
            return;
         }
         stopping = true;
         if (trace) log.trace(this + " is stopped.  No more listeners will be accepted.");
         
         Iterator itr = listeners.keySet().iterator();
         while (itr.hasNext())
         {
            final ConnectionListener listener = (ConnectionListener) itr.next();
            Set clients = (Set) listeners.get(listener);
            Iterator itr2 = clients.iterator();
            while (itr2.hasNext())
            {
               final Client client  = (Client) itr2.next();
               new Thread()
               {
                  public void run()
                  {
                     log.debug(ConnectionValidator.this + " calling " + listener + ".handleConnectionException() for " + client);
                     listener.handleConnectionException(t, client);
                  }
               }.start();
            }
         }
         
         listeners.clear();
      }
      
      stop();
   }
   
   // Protected ------------------------------------------------------------------------------------

   // Private --------------------------------------------------------------------------------------

   private void getParameters(Client client, Map metadata)
   {
      if (checkUseParametersFromLocator(client, metadata))
      {
         getParametersFromMap(client.getInvoker().getLocator().getParameters());
      }
      getParametersFromMap(client.getConfiguration());
      getParametersFromMap(metadata);
      
      ClientInvoker clientInvoker = client.getInvoker();
      if (clientInvoker instanceof MicroRemoteClientInvoker)
      {
         sharedInvoker = (MicroRemoteClientInvoker) clientInvoker;
         invokerSessionId = sharedInvoker.getSessionId();
      }
      else
      {
         throw new RuntimeException("creating a ConnectionValidator on a local connection");
      }
      if (stopLeaseOnFailure)
      {
         if (sharedInvoker != null)
         {
            leasePinger = sharedInvoker.getLeasePinger();
         }
      }
      if (trace) log.trace(this + ": sharedInvoker = " + sharedInvoker + ", leasePinger = " + leasePinger);
   }
   
   private boolean checkUseParametersFromLocator(Client client, Map metadata)
   {
      if (client.getInvoker() == null)
      {
         return false;
      }
      Object o = client.getInvoker().getLocator().getParameters().get(Client.USE_ALL_PARAMS);
      if (o != null)
      {
         if (o instanceof String)
         {
            return Boolean.valueOf(((String) o)).booleanValue();
         }
         else
         {
            log.warn(this + " could not convert " + Client.USE_ALL_PARAMS + " value" +
            " in InvokerLocator to a boolean: must be a String");
         }
      }
      o = client.getConfiguration().get(Client.USE_ALL_PARAMS);
      if (o != null)
      {
         if (o instanceof String)
         {
            return Boolean.valueOf(((String) o)).booleanValue();
         }
         else
         {
            log.warn(this + " could not convert " + Client.USE_ALL_PARAMS + " value" +
                     " in Client configuration map to a boolean: must be a String");
         }
      }
      o = metadata.get(Client.USE_ALL_PARAMS);
      if (o != null)
      {
         if (o instanceof String)
         {
            return Boolean.valueOf(((String) o)).booleanValue();
         }
         else
         {
            log.warn(this + " could not convert " + Client.USE_ALL_PARAMS + " value" +
                     " in metadata map to a boolean: must be a String");
         }
      }
      return false;
   }
   
   private void getParametersFromMap(Map config)
   {
      if (config != null)
      {  
         Object o = config.get(VALIDATOR_PING_PERIOD);
         if (o != null)
         {
            if (o instanceof String)
            {
               try 
               {
                  pingPeriod = Long.parseLong((String)o);
               }
               catch (Exception e)
               {
                  log.warn(this + " could not convert " + VALIDATOR_PING_PERIOD +
                           " value of " + o + " to a long value");
               }
            }
            else
            {
               log.warn(this + " could not convert " + VALIDATOR_PING_PERIOD +
                        " value of " + o + " to a long value: must be a String");
            }
         }

         o = config.get(VALIDATOR_PING_TIMEOUT);
         if (o != null)
         {
            if (o instanceof String)
            {
               try 
               {
                  pingTimeout = Integer.parseInt((String)o);
               }
               catch (Exception e)
               {
                  log.warn(this + " could not convert " + VALIDATOR_PING_TIMEOUT +
                           " value of " + o + " to a long value");
               }
            }
            else
            {
               log.warn(this + " could not convert " + VALIDATOR_PING_TIMEOUT +
                        " value of " + o + " to a long value: must be a String");
            }
         }
         
         o = config.get(TIE_TO_LEASE);
         if (o != null)
         {
            if (o instanceof String)
            {
               try
               {
                  tieToLease = Boolean.valueOf(((String) o)).booleanValue();
               }
               catch (Exception e)
               {
                  log.warn(this + " could not convert " + TIE_TO_LEASE + " value" +
                        " to a boolean: " + o);
               }
            }
            else
            {
               log.warn(this + " could not convert " + TIE_TO_LEASE + " value" +
               " to a boolean: must be a String");
            }
         }

         o = config.get(STOP_LEASE_ON_FAILURE);
         if (o != null)
         {
            if (o instanceof String)
            {
               try
               {
                  stopLeaseOnFailure = Boolean.valueOf(((String) o)).booleanValue();
               }
               catch (Exception e)
               {
                  log.warn(this + " could not convert " + STOP_LEASE_ON_FAILURE + " value" +
                        " to a boolean: " + o);
               }
            }
            else
            {
               log.warn(this + " could not convert " + STOP_LEASE_ON_FAILURE + " value" +
               " to a boolean: must be a String");
            }
         }
         
         o = config.get(FAILURE_DISCONNECT_TIMEOUT);
         if (trace) log.trace(this + " \"failureDisconnectTimeout\" set to " + o);
         if (o != null)
         {
            if (o instanceof String)
            {
               try
               {
                  failureDisconnectTimeout = Integer.valueOf(((String) o)).intValue();
                  if (trace) log.trace(this + " setting failureDisconnectTimeout to " + failureDisconnectTimeout);
               }
               catch (Exception e)
               {
                  log.warn(this + " could not convert " + FAILURE_DISCONNECT_TIMEOUT + " value" +
                        " to an int: " + o);
               }
            }
            else
            {
               log.warn(this + " could not convert " + FAILURE_DISCONNECT_TIMEOUT + " value" +
               " to an int: must be a String");
            }
         }
         o = config.get(Remoting.USE_CLIENT_CONNECTION_IDENTITY);
         if (o != null)
         {
            if (o instanceof String)
            {
               try
               {
                  useClientConnectionIdentity = Boolean.valueOf(((String) o)).booleanValue();
               }
               catch (Exception e)
               {
                  log.warn(this + " could not convert " + Remoting.USE_CLIENT_CONNECTION_IDENTITY + " value" +
                           " to a boolean: " + o);
               }
            }
            else
            {
               log.warn(this + " could not convert " + Remoting.USE_CLIENT_CONNECTION_IDENTITY + " value" +
                        " to a boolean: must be a String");
            }
         }
      }
   }
   
   private void start()
   {
      metadata.put(ServerInvoker.TIMEOUT, Integer.toString(pingTimeout));
      configMap = createPingConfig(client.getConfiguration(), metadata);
      log.debug(this + ": pingPeriod:  " + this.pingPeriod);
      log.debug(this + ": pingTimeout: " + this.pingTimeout);
      log.debug(this + ": ping retries: " + configMap.get("NumberOfCallRetries"));

      try
      {
         try
         {
            clientInvoker = (ClientInvoker) AccessController.doPrivileged( new PrivilegedExceptionAction()
            {
               public Object run() throws Exception
               {
                  return InvokerRegistry.createClientInvoker(locator, configMap);
               }
            });
         }
         catch (PrivilegedActionException pae)
         {
            throw pae.getException();
         }
      }
      catch (Exception e)
      {
         log.debug("Unable to create client invoker for locator: " + locator);
         throw new RuntimeException("Unable to create client invoker for locator: " + locator, e);
      }

      if (!clientInvoker.isConnected())
      {
         if (trace) { log.trace("inner client invoker not connected, connecting ..."); }
         clientInvoker.connect();
      }
      
      started = true;
      timer = new Timer(true);
      
      try
      {
          TimerUtil.schedule(this, pingPeriod);
      }
      catch (Exception e)
      {
         log.error(this + " unable to schedule on TimerUtil", e);
         started = false;
         timer = null;
         return;
      }
      log.debug(this + " started");
   }
   
   private boolean doCheckConnectionWithLease() throws Throwable
   {
      boolean pingWorked = false;

      try
      {
         Map metadata = new HashMap();
         metadata.put(ServerInvoker.INVOKER_SESSION_ID, invokerSessionId);
         InvocationRequest ir =
            new InvocationRequest(null, Subsystem.SELF, "$PING$", metadata, null, null);

         if (trace) { log.trace("pinging, sending " + ir + " over " + clientInvoker); }

         Object o = clientInvoker.invoke(ir);
         if (o instanceof Boolean && !((Boolean) o).booleanValue())
         {
            // Server indicates lease has stopped.
            throw new Exception();
         }

         if (trace) { log.trace("ConnectionValidator got successful ping using " + clientInvoker);}

         pingWorked = true;
      }
      catch (Throwable t)
      {
         log.debug("ConnectionValidator failed to ping via " + clientInvoker, t);
      }

      return pingWorked;
   }
   
   private boolean doCheckConnectionWithoutLease() throws Throwable
   {
      boolean pingWorked = false;

      try
      {
         // Sending null client id as don't want to trigger lease on server side. This also means
         // that client connection validator will NOT impact client lease, so can not depend on it
         // to maintain client lease with the server.
         InvocationRequest ir =
            new InvocationRequest(null, Subsystem.SELF, "$PING$", null, null, null);

         if (trace) { log.trace("pinging, sending " + ir + " over " + clientInvoker); }

         clientInvoker.invoke(ir);

         if (trace) { log.trace("ConnectionValidator got successful ping using " + clientInvoker);}

         pingWorked = true;
      }
      catch (Throwable t)
      {
         log.debug("ConnectionValidator failed to ping via " + clientInvoker, t);
      }

      return pingWorked;
   }

   private boolean doStop()
   {
      if (trace) log.trace("entering doStop()");
      synchronized(lock)
      {
         if (stopped)
         {
            return false;
         }
         
         if (!listeners.isEmpty())
         {
            listeners.clear();
         }
         stopping = true;
         stopped = true;
         timer = null;
      }

      if (clientInvoker != null)
      {
         AccessController.doPrivileged( new PrivilegedAction()
         {
            public Object run()
            {
               InvokerRegistry.destroyClientInvoker(locator, configMap);
               return null;
            }
         });
      }

      TimerUtil.unschedule(this);

      boolean result = super.cancel();
      log.debug(this + " stopped, returning " + result);
      return result;
   }
   
   // Inner classes --------------------------------------------------------------------------------

   private class WaitOnConnectionCheckTimerTask extends TimerTask
   {
      public void run()
      {
         long start = System.currentTimeMillis();
         synchronized (notificationLock)
         {
            while (true)
            {
               int elapsed = (int) (System.currentTimeMillis() - start);
               int wait = pingTimeout - elapsed;
               if (wait <= 0)
               {
                  break;
               }
               
               try
               {
                  notificationLock.wait(wait);
                  break;
               }
               catch (InterruptedException e)
               {
                  continue;
               }
            }
         }
         
         if (!isValid)
         {
            log.debug(ConnectionValidator.this + "'s connection is invalid");
            ConnectionValidator.super.cancel();
            
            if (stopLeaseOnFailure)
            {
               if (trace) log.trace(ConnectionValidator.this + " detected connection failure: stopping LeasePinger");
               if (leasePinger != null)
               {
                  log.debug(ConnectionValidator.this + " shutting down lease pinger: " + leasePinger);
                  int disconnectTimeout = (failureDisconnectTimeout == -1) ? client.getDisconnectTimeout() : failureDisconnectTimeout;
                  if (trace) log.trace(ConnectionValidator.this + " disconnectTimeout: " + disconnectTimeout);
                  sharedInvoker.terminateLease(null, disconnectTimeout, leasePinger);
               }
               else
               {
                  if (trace) log.trace(ConnectionValidator.this + ": lease pinger == null: perhaps leasing is not enabled for this connection");
                  notifyListeners(new Exception("Could not connect to server!"));
               }
               
               cancel();
            }
            if (!useClientConnectionIdentity)
            {
                notifyListeners(new Exception("Could not connect to server!"));
            }
         }
      }
   }
   
   private static class ConnectionCheckThread extends Thread
   {
      private InvocationRequest ir;
      private ClientInvoker clientInvoker;
      private boolean isValid;

      public ConnectionCheckThread(ClientInvoker clientInvoker, InvocationRequest ir)
      {
         this.clientInvoker = clientInvoker;
         this.ir = ir;
         setDaemon(true);
      }
      
      public void run()
      {
         try
         {
            if (trace) { log.trace("pinging, sending " + ir + " over " + clientInvoker); }
            clientInvoker.invoke(ir);
            isValid = true;
            if (trace) { log.trace("ConnectionValidator got successful ping using " + clientInvoker);}
         }
         catch (Throwable t)
         {
            log.debug("ConnectionValidator failed to ping via " + clientInvoker, t);
         }
      }
      
      public boolean isValid()
      {
         return isValid;
      }
   }
}