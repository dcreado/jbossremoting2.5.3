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

import org.jboss.logging.Logger;
import org.jboss.remoting.util.TimerUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;

/**
 * This class is used on the remoting server to maintain lease information
 * for remoting clients.  Will generate callback to ConnectionListener interface
 * if determined that client no longer available.
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class Lease
{

   private ConnectionNotifier notifier = null;
   private String clientSessionId = null;
   private long leasePeriod = -1;
   private String locatorURL = null;
   private Map requestPayload = null;
   private LeaseTimerTask leaseTimerTask = null;
   private long leaseWindow = -1;
   private long pingStart = -1;
   private Map clientLeases = null;
   private Object lock = new Object();
   private String leasePingerId;
   private boolean stopped;

   private boolean leaseUpdated = false;
   private long lastUpdate;
   private boolean useClientConnectionIdentity;

   private static final Logger log = Logger.getLogger(Lease.class);
   private static final boolean isTraceEnabled = log.isTraceEnabled();

   public Lease(String clientSessionId, long leasePeriod, String locatorurl, Map requestPayload,
                ConnectionNotifier notifier, Map clientLeases)
   {
      this.clientSessionId = clientSessionId;
      this.leasePeriod = leasePeriod;
      this.notifier = notifier;
      this.locatorURL = locatorurl;
      if(requestPayload != null)
      {
         this.requestPayload = (Map)requestPayload.get(ClientHolder.CLIENT_HOLDER_KEY);
         this.leasePingerId = (String) requestPayload.get(LeasePinger.LEASE_PINGER_ID);
         String s = (String) requestPayload.get(LeasePinger.TIME_STAMP);
         if (s != null)
         {
            this.lastUpdate = Long.valueOf(s).longValue();
            this.useClientConnectionIdentity = true;
            if (isTraceEnabled) log.trace(this + " initialized with lastUpdate: " + lastUpdate);
         }
         if (isTraceEnabled) log.trace(this + " initialized with requestPayload: " + this.requestPayload);
         if (isTraceEnabled) log.trace("leasePingerId: " + leasePingerId);
      }
      this.leaseWindow = leasePeriod * 2;
      this.clientLeases = clientLeases;
   }


   public void startLease()
   {
      if(isTraceEnabled)
      {
         log.trace("Starting lease for client invoker (session id = " + clientSessionId + ") with lease window time of " + leaseWindow);
      }
      leaseTimerTask = new LeaseTimerTask();
      TimerUtil.schedule(leaseTimerTask, leaseWindow);
   }

   public void updateLease(long leasePeriod, Map requestMap)
   {
      if(requestMap != null)
      {
         synchronized (lock)
         {
            if (useClientConnectionIdentity)
            {
               if (isTraceEnabled) log.trace(this + " requestMap: " + requestMap);
               long time = 0;
               String timeString = (String) requestMap.get(LeasePinger.TIME_STAMP);
               time = Long.valueOf(timeString).longValue();
               if (isTraceEnabled) log.trace(this + " last update: " + lastUpdate + ", this update: " + time);
               if (time >= lastUpdate)
               {
                  lastUpdate = time;
                  doUpdate(requestMap);
               }
               else
               {
                  if (isTraceEnabled) log.trace(this + " updating lease but not client list");
                  leaseUpdated = true;
               }
            }
            else
            {
               doUpdate(requestMap);
            }
         }
      }
      else
      {
         if (isTraceEnabled) log.trace(this + " requestPayload == null");
      }
   }

   public void updateLease(long leasePeriod)
   {
      leaseUpdated = true;
      if (leasePeriod != this.leasePeriod)
      {
         this.leasePeriod = leasePeriod;
         this.leaseWindow = leasePeriod * 2;
         stopLease();
         startLease();
         if(isTraceEnabled)
         {
            log.trace("Lease for client invoker (session id = " + clientSessionId + ") updated with new lease window of " + leaseWindow + ".  Resetting timer.");
         }
      }
      else
      {
         if (pingStart != -1)
         {
            long pingDuration = System.currentTimeMillis() - pingStart;
            if (pingDuration > 0.75 * leaseWindow)
            {
               leaseWindow = pingDuration * 2;

               stopLease();
               leaseTimerTask = new LeaseTimerTask();
               TimerUtil.schedule(leaseTimerTask, leaseWindow);
            }
         }

      }
      pingStart = System.currentTimeMillis();
   }

   public void terminateLease(String sessionId)
   {
      // is this terminate for all clients
      if (clientSessionId.equals(sessionId))
      {
         if(isTraceEnabled)
         {
            log.trace(this + " Terminating lease group for session id " + sessionId);
         }
         
         stopLease();
         // should be ok to call this will null as all the client should have
         // already been disconnected and there been a notification for each
         // of these client disconnections (which would remove the client from
         // the lease, thus leaving the collection empty
         notifyClientTermination(null);
      }
      else
      {
         if(isTraceEnabled)
         {
            log.trace(this + " Terminating individual lease for session id " + sessionId);
         }
         notifyClientTermination(sessionId);
      }
   }
   
   public void terminateLeaseUponFailure(String sessionId)
   {
      // is this terminate for all clients
      if (clientSessionId.equals(sessionId))
      {
         if(isTraceEnabled)
         {
            log.trace(this + " Terminating lease group for session id " + sessionId);
         }
         
         stopLease();
         // should be ok to call this will null as all the client should have
         // already been disconnected and there been a notification for each
         // of these client disconnections (which would remove the client from
         // the lease, thus leaving the collection empty
         notifyClientLost();
      }
      else
      {
         if(true)
         {
            log.warn(this + " Expected invoker session id: " + sessionId);
         }
         notifyClientLost();
      }
   }
   
   public String toString()
   {
      String hash = Integer.toHexString(System.identityHashCode(this));
      return "Lease[" + hash + ":" + clientSessionId + ":" + leasePingerId + "]";
   }
   
   private void notifyClientTermination(String sessionId)
   {
      Map localRequestPayload = null;
      synchronized (lock)
      {
         if (requestPayload != null)
         {  
            localRequestPayload = new HashMap(requestPayload);
            if (sessionId != null)
            {
               requestPayload.remove(sessionId);
            }
         }
      }
      
      if (localRequestPayload != null)
      {  
         // should notify for one client or all?
         if (sessionId != null)
         {
            synchronized (lock)
            {
               if (stopped)
               {
                  if (isTraceEnabled) log.trace(this + " already stopped");
                  return;
               }
            }
            
            Object clientHolderObj =  localRequestPayload.get(sessionId);
            if (clientHolderObj != null && clientHolderObj instanceof ClientHolder)
            {
               ClientHolder clientHolder = (ClientHolder) clientHolderObj;
               notifier.connectionTerminated(locatorURL, clientHolder.getSessionId(), clientHolder.getConfig());
               if(isTraceEnabled)
               {
                  log.trace(this + " Notified connection listener of lease termination due to disconnect from client (client session id = " + clientHolder.getSessionId());
               }
            }
         }
         else
         {
            synchronized (lock)
            {
               if (stopped)
               {
                  if (isTraceEnabled) log.trace(this + " already stopped");
                  return;
               }
               stopped = true;
            }
            
            // loop through and notify for all clients
            Collection clientHoldersCol = localRequestPayload.values();
            if (clientHoldersCol != null && clientHoldersCol.size() > 0)
            {
               Iterator itr = clientHoldersCol.iterator();
               while (itr.hasNext())
               {
                  Object val = itr.next();
                  if (val != null && val instanceof ClientHolder)
                  {
                     ClientHolder clientHolder = (ClientHolder) val;
                     notifier.connectionTerminated(locatorURL, clientHolder.getSessionId(), clientHolder.getConfig());
                     if(isTraceEnabled)
                     {
                        log.trace(this + " Notified connection lif (isTraceEnabled) log.tracef lease termination due to disconnect from client (client session id = " + clientHolder.getSessionId());
                     }
                  }
               }
            }
         }
      }
      else
      {
         log.warn(this + " Tried to terminate lease for session id " + sessionId + ", but no collection of clients have been set.");
      }
   }

   private void notifyClientLost()
   {
      Map localRequestPayload = null;
      synchronized (lock)
      {
         if (stopped)
         {
            if (isTraceEnabled) log.trace(this + " already stopped");
            return;
         }
         stopped = true;
         if (requestPayload != null)
         {  
            localRequestPayload = new HashMap(requestPayload);
         }
      }
      
      if (localRequestPayload != null)
      {
         // loop through and notify for all clients
         Collection clientHoldersCol = localRequestPayload.values();
         if (isTraceEnabled) log.trace(this + " notifying listeners about " + clientHoldersCol.size() + " expired client(s)");
         if (clientHoldersCol != null && clientHoldersCol.size() > 0)
         {
            Iterator itr = clientHoldersCol.iterator();
            while (itr.hasNext())
            {
               Object val = itr.next();
               if (val != null && val instanceof ClientHolder)
               {
                  ClientHolder clientHolder = (ClientHolder) val;
                  notifier.connectionLost(locatorURL, clientHolder.getSessionId(), clientHolder.getConfig());
                  if(isTraceEnabled)
                  {
                     log.trace(this + " Notified connection listener of lease expired due to lost connection from client (client session id = " + clientHolder.getSessionId());
                  }
               }
            }
         }
      }
      else
      {
         if (isTraceEnabled) log.trace(this + " requestPayload == null, calling ConnectionNotifier.connectionLost()");
         notifier.connectionLost(locatorURL, clientSessionId, null);
      }
   }

   protected String getLeasePingerId()
   {
      return leasePingerId;
   }

   private void stopLease()
   {
      leaseTimerTask.cancel();
   }

   private void doUpdate(Map requestMap)
   {
      this.requestPayload = (Map)requestMap.get(ClientHolder.CLIENT_HOLDER_KEY);
      if (isTraceEnabled) 
      {
         log.trace(this + " updating: new Client list:");
         Collection clientHoldersCol = requestPayload.values();
         Iterator itr = clientHoldersCol.iterator();
         while (itr.hasNext())
         {
            Object val = itr.next();
            if (val != null && val instanceof ClientHolder)
            {
               ClientHolder clientHolder = (ClientHolder) val;
               log.trace(leasePingerId + ":  " + clientHolder.getSessionId());
            }
         }
      }
      updateLease(leasePeriod);
   }
   
   
   private class LeaseTimerTask extends TimerTask
   {

      /**
       * The action to be performed by this timer task.
       */
      public void run()
      {
         if (leaseUpdated)
         {
            leaseUpdated = false;
         }
         else
         {
            try
            {
               if (isTraceEnabled) log.trace(Lease.this + " did not receive ping: " + clientSessionId);
               stopLease();
               notifyClientLost();
               if (clientLeases != null)
               {
                  clientLeases.remove(clientSessionId);
               }
               if (isTraceEnabled) log.trace(Lease.this + " removed lease:" + clientSessionId);
            }
            catch (Throwable thr)
            {
               log.error("Error terminating client lease and sending notification of lost client.", thr);
            }
         }
      }
   }
}