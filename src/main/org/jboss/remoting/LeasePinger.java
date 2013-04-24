package org.jboss.remoting;

import org.jboss.logging.Logger;
import org.jboss.remoting.transport.ClientInvoker;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

/**
 * Internal agent class to ping the remote server to keep lease alive.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @author <a href="mailto:ovidiu@ejboss.org">Ovidiu Feodorov</a>
 */
public class LeasePinger
{
   // Constants ------------------------------------------------------------------------------------

   private static final Logger log = Logger.getLogger(LeasePinger.class);

   public static final long DEFAULT_LEASE_PERIOD = 5000;
   public static final int DEFAULT_DISCONNECT_TIMEOUT = -1;
   public static final String LEASE_PINGER_TIMEOUT = "leasePingerTimeout";

   static final String LEASE_PINGER_ID = "leasePingerId";
   static final String TIME_STAMP = "timeStamp";
   
   // Static ---------------------------------------------------------------------------------------

   private static boolean trace = log.isTraceEnabled();

   private static Timer timer = new Timer(true);

   // Attributes -----------------------------------------------------------------------------------

   private long defaultPingPeriod = -1;

   private ClientInvoker invoker = null;
   private String invokerSessionID = null;

   private Map clientSessionIds = new ConcurrentHashMap();
   private Map clients = new ConcurrentHashMap();
   private TimerTask timerTask = null;

   private long pingPeriod = -1;
   private int disconnectTimeout = DEFAULT_DISCONNECT_TIMEOUT;
   private int leasePingerTimeout = -1;
   
   // The following variables exist for testing purposes.
   private boolean pingInvoked;
   private boolean pingSucceeded;
   
   private String leasePingerId;
   private boolean useClientConnectionIdentity;

   // Constructors ---------------------------------------------------------------------------------

   public LeasePinger(ClientInvoker invoker, String invokerSessionID, long defaultLeasePeriod)
   {
      this(invoker, invokerSessionID, defaultLeasePeriod, null);
   }
   
   public LeasePinger(ClientInvoker invoker, String invokerSessionID, long defaultLeasePeriod, Map config)
   {
      this.invoker = invoker;
      this.invokerSessionID = invokerSessionID;
      this.pingPeriod = defaultLeasePeriod;
      this.defaultPingPeriod = defaultLeasePeriod;
      
      if (config != null)
      {
         Object o = config.get(LEASE_PINGER_TIMEOUT);
         if (o != null)
         {
            if (o instanceof String)
            {
               try
               {
                  leasePingerTimeout = Integer.valueOf((String) o).intValue();
               }
               catch (NumberFormatException  e)
               {
                  log.warn("leasePingerTimeout parameter must represent an int: " + o);
               }
            }
            else
            {
               log.warn("leasePingerTimeout parameter must be a String representing an int");
            }
         }
      }
   }

   // Public ---------------------------------------------------------------------------------------

   public void startPing()
   {
      if(trace) { log.trace(this + " starting lease timer with ping period of " + pingPeriod); }

      timerTask = new LeaseTimerTask(this);

      try
      {
         timer.schedule(timerTask, pingPeriod, pingPeriod);
      }
      catch (IllegalStateException e)
      {
         log.debug("Unable to schedule TimerTask on existing Timer", e);
         timer = new Timer(true);
         timer.schedule(timerTask, pingPeriod, pingPeriod);
      }
   }

   public void stopPing()
   {
      if(trace) { log.trace(this + " stopping lease timer"); }

      if (timerTask != null)
      {
         timerTask.cancel();
         timerTask = null;
         
         if (useClientConnectionIdentity)
         {
            Iterator it = clients.values().iterator();
            while (it.hasNext())
            {
               Client client = (Client) it.next();
               if (trace) log.trace(this + " calling " + client + ".notifyAndDisconnect()");
               client.notifyListeners();
               it.remove();
            }
         }
         
         try
         {
            // sending request map with no ClientHolders will indicate to server
            // that is full disconnect (for client invoker)
            HashMap metadata = null;
            
            // If disconnectTimeout == 0, skip network i/o.
            if (trace) log.trace(this + ": disconnectTimeout: " + disconnectTimeout);
            if (disconnectTimeout != 0)
            {
               if (disconnectTimeout > 0)
               {
                  metadata = new HashMap(1);
                  metadata.put(ServerInvoker.TIMEOUT, Integer.toString(disconnectTimeout));
               }
               InvocationRequest ir =
                  new InvocationRequest(invokerSessionID, null, "$DISCONNECT$", metadata, null, null);
               invoker.invoke(ir);
            }
         }
         catch (Throwable throwable)
         {
            RuntimeException e = new RuntimeException("Error tearing down lease with server.");
            e.initCause(throwable);
            throw e;
         }
         
         if (trace) 
         {
            log.trace(this + " shut down");
            if (!clientSessionIds.isEmpty())
            {
               log.trace(this + " " + clientSessionIds.size() + " remaining clients:");
               Iterator it = clientSessionIds.keySet().iterator();
               while (it.hasNext())
               {
                  log.trace(this + ": " + it.next());
               }
               clientSessionIds.clear();
            }
            else
            {
               log.trace(this + " No remaining clients");
            }
         }
      }
   }

   public void addClient(String sessionID, Map configuration, long leasePeriod)
   {
      if (leasePeriod <= 0)
      {
         leasePeriod = defaultPingPeriod;
      }

      if(trace) { log.trace(this + " adding new client with session ID " + sessionID + " and lease period " + leasePeriod); }

      if (useClientConnectionIdentity)
      {
         Client client = (Client) configuration.remove(Client.CLIENT);
         if (client != null)
         {
            clients.put(sessionID, client);
         }
      }
      
      ClientHolder newClient = new ClientHolder(sessionID, configuration, leasePeriod);
      clientSessionIds.put(sessionID, newClient);

      try
      {
         sendClientPing();
      }
      catch (Throwable t)
      {
         log.debug(this + " failed to ping to server", t);
         log.warn(this + " failed to ping to server: " + t.getMessage());
         throw new RuntimeException(t);
      }
      // if new client lease period is less than the current ping period, need to refresh to new one
      if (leasePeriod < pingPeriod)
      {
         pingPeriod = leasePeriod;

         // don't want to call stopPing() as that will send disconnect for client invoker
         if (timerTask != null)
         {
            timerTask.cancel();
            timerTask = null;
            startPing();
         }
      }
   }

   public boolean removeClient(String sessionID)
   {
      boolean isLastClientLease = false;

      if(trace) { log.trace(this + " removing client with session ID " + sessionID); }

      // Don't remove holder until after client has been removed from server side Lease, to
      // avoid a race with LeaseTimerTask sending a PING without the Client being removed.
      ClientHolder holder = (ClientHolder)clientSessionIds.get(sessionID);
      
      if (holder != null)
      {
         // send disconnect for this client
         try
         {
            Map clientMap = new HashMap();
            clientMap.put(ClientHolder.CLIENT_HOLDER_KEY, holder);
            
            // If disconnectTimeout == 0, skip network i/o.
            if (disconnectTimeout != 0)
            {
               if (disconnectTimeout > 0)
                  clientMap.put(ServerInvoker.TIMEOUT, Integer.toString(disconnectTimeout));
               
               InvocationRequest ir = new InvocationRequest(invokerSessionID, null, "$DISCONNECT$",
                     clientMap, null, null);
               invoker.invoke(ir);
               
               if(trace) { log.trace(this + " sent out disconnect message to server for lease tied to client with session ID " + sessionID); }
            }
         }
         catch (Throwable throwable)
         {
            log.debug(this + " failed sending disconnect for client lease for " +
                  "client with session ID " + sessionID);
         }
         
         clientSessionIds.remove(sessionID);
         if (useClientConnectionIdentity)
         {
            clients.remove(sessionID);
         }
      }
      else
      {
         log.debug(this + " tried to remove lease for client with session ID " + sessionID +
                   ", but no such lease was found: probably it was registered with an older LeasePinger");
      }
      
      if (clientSessionIds.isEmpty())
      {
         isLastClientLease = true;
         if(trace) { log.trace(this + " has no more client leases"); }
      }
      else
      {
         // now need to see if any of the other client holders have a lower lease period than
         // default

         long tempPingPeriod = defaultPingPeriod;

         for (Iterator i = clientSessionIds.values().iterator(); i.hasNext(); )
         {
            ClientHolder clientHolder = (ClientHolder)i.next();
            long clientHolderLeasePeriod = clientHolder.getLeasePeriod();
            if (clientHolderLeasePeriod > 0 && clientHolderLeasePeriod < tempPingPeriod)
            {
               tempPingPeriod = clientHolderLeasePeriod;
            }
         }

         // was there a change in lease period?
         if (tempPingPeriod != pingPeriod)
         {
            // need to update to new ping period and reset timer
            pingPeriod = tempPingPeriod;

            if (timerTask != null)
            {
               timerTask.cancel();
               timerTask = null;
            }
            startPing();
         }

      }
      return isLastClientLease;
   }

   public long getLeasePeriod(String sessionID)
   {
      if (timerTask == null)
      {
         return -1;
      }

      // look to see if the client is still amont those serviced by this lease pinger
      if (clientSessionIds.containsKey(sessionID))
      {
         return pingPeriod;
      }
      else
      {
         return -1;
      }
   }

   public String toString()
   {
      return "LeasePinger[" + leasePingerId + ":" + invoker + "(" + invokerSessionID + ")]";
   }

   // Package protected ----------------------------------------------------------------------------

   // Protected ------------------------------------------------------------------------------------

   
   protected int getDisconnectTimeout()
   {
      return disconnectTimeout;
   }
   
   protected void setDisconnectTimeout(int disconnectTimeout)
   {
      this.disconnectTimeout = disconnectTimeout;
      if (trace) log.trace(this + " setting disconnect timeout to: " + disconnectTimeout);
   }
   
   protected String getLeasePingerId()
   {
      return leasePingerId;
   }

   protected void setLeasePingerId(String leasePingerId)
   {
      this.leasePingerId = leasePingerId;
   }
   
   boolean isUseClientConnectionIdentity()
   {
      return useClientConnectionIdentity;
   }

   void setUseClientConnectionIdentity(boolean useClientConnectionIdentity)
   {
      this.useClientConnectionIdentity = useClientConnectionIdentity;
   }
   
   // Private --------------------------------------------------------------------------------------

   private void sendClientPing() throws Throwable
   {
      if(trace)
      {
         StringBuffer sb = new StringBuffer();
         if(clientSessionIds != null)
         {
            for(Iterator i = clientSessionIds.values().iterator(); i.hasNext(); )
            {
               ClientHolder h = (ClientHolder)i.next();
               sb.append("    ").append(h.getSessionId()).append('\n');
            }
         }

         log.trace(this + " sending ping to server. Currently managing lease " +
               "for following clients:\n" + sb.toString());
      }

      Map clientsClone = new ConcurrentHashMap(clientSessionIds);
      Map requestClients = new ConcurrentHashMap();
      requestClients.put(ClientHolder.CLIENT_HOLDER_KEY, clientsClone);
      requestClients.put(LeasePinger.LEASE_PINGER_ID, leasePingerId);
      requestClients.put(TIME_STAMP, Long.toString(System.currentTimeMillis()));

      if (leasePingerTimeout >= 0)
      {
         requestClients.put(ServerInvoker.TIMEOUT, Integer.toString(leasePingerTimeout));
      }
      
      InvocationRequest ir = new InvocationRequest(invokerSessionID, null, "$PING$", requestClients, null, null);
      
      pingSucceeded = false;
      pingInvoked = true;
      invoker.invoke(ir);

      pingSucceeded = true;
      pingInvoked = false;
      if(trace) { log.trace(this + " successfully pinged the server"); }
   }

   // Inner classes --------------------------------------------------------------------------------

   static private class LeaseTimerTask extends TimerTask
   {
      private LeasePinger pinger;

      LeaseTimerTask(final LeasePinger pinger)
      {
          this.pinger = pinger;
      }

      public void run()
      {
         final LeasePinger currentPinger;
         synchronized(this)
         {
             currentPinger = pinger;
         }

         if (currentPinger != null)
         {
            try
            {
               currentPinger.sendClientPing();
            }
            catch (Throwable t)
            {
               log.debug(this + " failed to ping to server", t);
               log.warn(this + " failed to ping to server: " + t.getMessage());
            }
         }
      }

      public boolean cancel()
      {
          synchronized(this)
          {
              pinger = null;
          }
          return super.cancel();
      }
   }
}
