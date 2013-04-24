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

package org.jboss.remoting.transport.bisocket;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.Socket;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.ConnectionFailedException;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.invocation.InternalInvocation;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.transport.BidirectionalClientInvoker;
import org.jboss.remoting.transport.socket.SocketClientInvoker;
import org.jboss.remoting.transport.socket.SocketWrapper;
import org.jboss.remoting.util.SecurityUtility;

import EDU.oswego.cs.dl.util.concurrent.Semaphore;

/**
 * The bisocket transport, an extension of the socket transport, is designed to allow
 * a callback server to function behind a firewall.  All connections are created by
 * a Socket constructor or factory on the client side connecting to a ServerSocket on
 * the server side.  When a callback client invoker on the server side needs to
 * open a connection to the callback server, it requests a connection by sending a
 * request message over a control connection to the client side.
 *
 * Because all connections are created in one direction, the bisocket transport is
 * asymmetric, in the sense that client invokers and server invokers behave differently
 * on the client side and on the server side.
 *
 *
 *
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 */
public class BisocketClientInvoker
extends SocketClientInvoker
implements BidirectionalClientInvoker
{
   private static final Logger log = Logger.getLogger(BisocketClientInvoker.class);
   private static Map listenerIdToClientInvokerMap = Collections.synchronizedMap(new HashMap());
   private static Map listenerIdToCallbackClientInvokerMap = Collections.synchronizedMap(new HashMap());
   private static Map listenerIdToSocketsMap = new HashMap();
   private static Map listenerIdToControlSocketsMap = new HashMap();
   private static Timer timer;
   private static Object timerLock = new Object();

   protected String listenerId;

   private int pingFrequency = Bisocket.PING_FREQUENCY_DEFAULT;
   private int pingWindowFactor = Bisocket.PING_WINDOW_FACTOR_DEFAULT;
   private int pingWindow = pingWindowFactor * pingFrequency;
   private int maxRetries = Bisocket.MAX_RETRIES_DEFAULT;
   private Socket controlSocket;
   private OutputStream controlOutputStream;
   private Object controlLock = new Object();
   private PingTimerTask pingTimerTask;
   protected boolean isCallbackInvoker;
   protected BooleanHolder pingFailed = new BooleanHolder(false);


   /**
    * @param listenerId
    * @return
    */
   static BisocketClientInvoker getBisocketClientInvoker(String listenerId)
   {
      return (BisocketClientInvoker) listenerIdToClientInvokerMap.get(listenerId);
   }


   static BisocketClientInvoker getBisocketCallbackClientInvoker(String listenerId)
   {
      return (BisocketClientInvoker) listenerIdToCallbackClientInvokerMap.get(listenerId);
   }
   
   
   static void removeBisocketClientInvoker(String listenerId)
   {
      listenerIdToClientInvokerMap.remove(listenerId);
   }


   static void transferSocket(String listenerId, Socket socket, boolean isControlSocket)
   {
      Set sockets = null;
      
      if (isControlSocket)
      {
         synchronized (listenerIdToControlSocketsMap)
         {
            sockets = (Set) listenerIdToControlSocketsMap.get(listenerId);
            if (sockets == null)
            {
               sockets = new HashSet();
               listenerIdToControlSocketsMap.put(listenerId, sockets);
            }
         }
      }
      else
      {
         synchronized (listenerIdToSocketsMap)
         {
            sockets = (Set) listenerIdToSocketsMap.get(listenerId);
            if (sockets == null)
            {
               sockets = new HashSet();
               listenerIdToSocketsMap.put(listenerId, sockets);
            }
         }
      }

      synchronized (sockets)
      {
         sockets.add(socket);
         sockets.notify();
      }
   }


   public BisocketClientInvoker(InvokerLocator locator) throws IOException
   {
      this(locator, null);
   }


   public BisocketClientInvoker(InvokerLocator locator, Map config) throws IOException
   {
      super(locator, config);

      if (configuration != null)
      {
         listenerId = (String) configuration.get(Client.LISTENER_ID_KEY);
         if (listenerId != null)
         {
            isCallbackInvoker = true;
            listenerIdToCallbackClientInvokerMap.put(listenerId, this);
            log.debug(this + " :registered " + listenerId + " -> " + this);
         }

         // look for pingFrequency param
         Object val = configuration.get(Bisocket.PING_FREQUENCY);
         if (val != null)
         {
            try
            {
               int nVal = Integer.valueOf((String) val).intValue();
               pingFrequency = nVal;
               log.debug("Setting ping frequency to: " + pingFrequency);
            }
            catch (Exception e)
            {
               log.warn("Could not convert " + Bisocket.PING_FREQUENCY +
                     " value of " + val + " to an int value.");
            }
         }
         
         val = configuration.get(Bisocket.PING_WINDOW_FACTOR);
         if (val != null && val instanceof String && ((String) val).length() > 0)
         {
            try
            {
               pingWindowFactor = Integer.valueOf(((String) val)).intValue();
               log.debug(this + " setting pingWindowFactor to " + pingWindowFactor);
            }
            catch (NumberFormatException e)
            {
               log.warn("Invalid format for " + "\"" + Bisocket.PING_WINDOW_FACTOR + "\": " + val);
            }
         }
         else if (val != null)
         {
            log.warn("\"" + Bisocket.PING_WINDOW_FACTOR + "\" must be specified as a String");
         }
         
         pingWindow = pingWindowFactor * pingFrequency;
         
         val = configuration.get(Bisocket.MAX_RETRIES);
         if (val != null)
         {
            try
            {
               int nVal = Integer.valueOf((String) val).intValue();
               maxRetries = nVal;
               log.debug("Setting retry limit: " + maxRetries);
            }
            catch (Exception e)
            {
               log.warn("Could not convert " + Bisocket.MAX_RETRIES +
                     " value of " + val + " to an int value.");
            }
         }
      }
   }

   public int getMaxRetries()
   {
      return maxRetries;
   }


   public void setMaxRetries(int maxRetries)
   {
      this.maxRetries = maxRetries;
   }
   

   public int getPingFrequency()
   {
      return pingFrequency;
   }


   public void setPingFrequency(int pingFrequency)
   {
      this.pingFrequency = pingFrequency;
   }

   
   public int getPingWindowFactor()
   {
      return pingWindowFactor;
   }
   
   
   public void setPingWindowFactor(int pingWindowFactor)
   {
      this.pingWindowFactor = pingWindowFactor;
      pingWindow = pingWindowFactor * pingFrequency;
   }
   
   
   protected void handleConnect() throws ConnectionFailedException
   {
      // Callback client on server side.
      if (isCallbackInvoker)
      {
         Set sockets = null;

         synchronized (listenerIdToControlSocketsMap)
         {
            sockets = (Set) listenerIdToControlSocketsMap.get(listenerId);
            if (sockets == null)
            {
               sockets = new HashSet();
               listenerIdToControlSocketsMap.put(listenerId, sockets);
            }
         }

         synchronized (sockets)
         {
            if (sockets.isEmpty())
            {
               long wait = timeout; 
               long start = System.currentTimeMillis(); 
               
               while (timeout == 0 || wait > 0)
               {
                  try
                  {
                     sockets.wait(wait);
                     break;
                  }
                  catch (InterruptedException e)
                  {
                     log.debug("unexpected interrupt");
                     if (timeout > 0)
                        wait = timeout - (System.currentTimeMillis() - start);
                  }
               }
            }
            
            if (sockets.isEmpty())
               throw new ConnectionFailedException("Timed out trying to create control socket");

            Iterator it = sockets.iterator();
            controlSocket = (Socket) it.next();
            it.remove();
            try
            {
               controlOutputStream = controlSocket.getOutputStream();
            }
            catch (IOException e1)
            {
               throw new ConnectionFailedException("Unable to get control socket output stream");
            }
            log.debug("got control socket( " + listenerId + "): " + controlSocket);

            if (pingFrequency > 0)
            {
               pingTimerTask = new PingTimerTask(this);

               synchronized (timerLock)
               {
                  if (timer == null)
                  {
                     timer = new Timer(true);
                  }
                  try
                  {
                     timer.schedule(pingTimerTask, pingFrequency, pingFrequency);
                  }
                  catch (IllegalStateException e)
                  {
                     log.debug("Unable to schedule TimerTask on existing Timer", e);
                     timer = new Timer(true);
                     timer.schedule(pingTimerTask, pingFrequency, pingFrequency);
                  }
               }
            }
         }

         // Bisocket callback client invoker doesn't share socket pools because of the danger
         // that two distinct callback servers could have the same "artifical" port.
         pool = new LinkedList();
         log.debug("Creating semaphore with size " + maxPoolSize);
         semaphore = new Semaphore(maxPoolSize);
         return;
      }

      // Client on client side.
      super.handleConnect();
   }
   
   
   protected void handleDisconnect()
   {
      if (listenerId != null)
      {
         if (isCallbackInvoker)
         {
            if (controlSocket != null)
            {
               try
               {
                  controlSocket.close();
               }
               catch (IOException e)
               {
                  log.debug("unable to close control socket: " + controlSocket);
               }
            }

            listenerIdToCallbackClientInvokerMap.remove(listenerId);
            for (Iterator it = pool.iterator(); it.hasNext();)
            {
               SocketWrapper socketWrapper = (SocketWrapper) it.next();
               try
               {
                  socketWrapper.close();
               }
               catch (Exception ignored)
               {
               }
            }
         }
         else
         {
            listenerIdToClientInvokerMap.remove(listenerId);
            super.handleDisconnect();
         }

         synchronized (listenerIdToControlSocketsMap)
         {
            listenerIdToControlSocketsMap.remove(listenerId);
         }
         
         Set sockets = null;
         synchronized (listenerIdToSocketsMap)
         {
            sockets = (Set) listenerIdToSocketsMap.remove(listenerId);
         }
         
         // Wake up any threads blocked in createSocket().
         if (sockets != null)
         {
            synchronized (sockets)
            {
               sockets.notifyAll();
            }
         }
         
         if (pingTimerTask != null)
            pingTimerTask.shutDown();
      }
      else
      {
         super.handleDisconnect();
      }
   }


   protected Object transport(String sessionId, Object invocation, Map metadata,
                              Marshaller marshaller, UnMarshaller unmarshaller)
   throws IOException, ConnectionFailedException, ClassNotFoundException
   {
      String listenerId = null;
      if (invocation instanceof InvocationRequest)
      {
         InvocationRequest ir = (InvocationRequest) invocation;
         Object o = ir.getParameter();
         if (o instanceof InternalInvocation)
         {
            InternalInvocation ii = (InternalInvocation) o;
            if (InternalInvocation.ADDLISTENER.equals(ii.getMethodName())
                && ir.getLocator() != null) // getLocator() == null for pull callbacks
            {
               Map requestPayload = ir.getRequestPayload();
               listenerId = (String) requestPayload.get(Client.LISTENER_ID_KEY);
               listenerIdToClientInvokerMap.put(listenerId, this);
               BisocketServerInvoker callbackServerInvoker;
               callbackServerInvoker = BisocketServerInvoker.getBisocketServerInvoker(listenerId);
               callbackServerInvoker.createControlConnection(listenerId, true);
            }
            
            // Rather than handle the REMOVELISTENER case symmetrically, it is
            // handled when a REMOVECLIENTLISTENER message is received by
            // BisocketServerInvoker.handleInternalInvocation().  The reason is that
            // if the Client executes removeListener() with disconnectTimeout == 0, 
            // no REMOVELISTENER message will be sent.
         }
      }

      return super.transport(sessionId, invocation, metadata, marshaller, unmarshaller);
   }


   protected Socket createSocket(String address, int port, int timeout) throws IOException
   {
      if (!isCallbackInvoker)
         return super.createSocket(address, port, timeout);

      if (timeout < 0)
      {
         timeout = getTimeout();
         if (timeout < 0)
            timeout = 0;
      }
      
      Set sockets = null;

      synchronized (listenerIdToSocketsMap)
      {
         sockets = (Set) listenerIdToSocketsMap.get(listenerId);

         if (sockets == null)
         {
            sockets = new HashSet();
            listenerIdToSocketsMap.put(listenerId, sockets);
         }
      }

      synchronized (controlLock)
      {
         if (log.isTraceEnabled()) log.trace(this + " writing Bisocket.CREATE_ORDINARY_SOCKET on " + controlOutputStream);
         try
         {
            controlOutputStream.write(Bisocket.CREATE_ORDINARY_SOCKET);
            if (log.isTraceEnabled()) log.trace(this + " wrote Bisocket.CREATE_ORDINARY_SOCKET");
            
            synchronized (sockets)
            {
               if (!sockets.isEmpty())
               {
                  Iterator it = sockets.iterator();
                  Socket socket = (Socket) it.next();
                  it.remove();
                  log.debug(this + " found socket (" + listenerId + "): " + socket);
                  return socket;
               }
            }
         }
         catch (IOException e)
         {
            log.debug(this + " unable to write Bisocket.CREATE_ORDINARY_SOCKET", e);
         }
      }
      
      long timeRemaining = timeout; 
      long pingFailedWindow = 2 * pingWindow;
      long pingFailedTimeRemaining = pingFailedWindow;
      long start = System.currentTimeMillis();
      OutputStream savedControlOutputStream = controlOutputStream;

      while (isConnected() && (!pingFailed.flag || pingFailedTimeRemaining > 0) && (timeout == 0 || timeRemaining > 0))
      {
         synchronized (sockets)
         {  
            try
            {
               sockets.wait(1000);
            }
            catch (InterruptedException e)
            {
               log.debug(this + " unexpected interrupt");
            }
            
            if (!sockets.isEmpty())
            {
               Iterator it = sockets.iterator();
               Socket socket = (Socket) it.next();
               it.remove();
               log.debug(this + " found socket (" + listenerId + "): " + socket);
               return socket;
            }
         }
         
         if (savedControlOutputStream != controlOutputStream)
         {
            savedControlOutputStream = controlOutputStream;
            log.debug(this + " rewriting Bisocket.CREATE_ORDINARY_SOCKET on " + controlOutputStream);
            try
            {
               controlOutputStream.write(Bisocket.CREATE_ORDINARY_SOCKET);
               log.debug(this + " rewrote Bisocket.CREATE_ORDINARY_SOCKET");
            }
            catch (IOException e)
            {
               log.debug(this + " unable to rewrite Bisocket.CREATE_ORDINARY_SOCKET" + e.getMessage());
            }
         }
         
         long elapsed = System.currentTimeMillis() - start;
         if (timeout > 0)
            timeRemaining = timeout - elapsed;
         pingFailedTimeRemaining = pingFailedWindow - elapsed; 
      }

      if (!isConnected())
      {
         throw new IOException("Connection is closed");
      }
      
      if (pingFailed.flag)
      {
         throw new IOException("Unable to create socket");
      }

      throw new IOException("Timed out trying to create socket");
   }


   void replaceControlSocket(Socket socket) throws IOException
   {
      synchronized (controlLock)
      {
         if (controlSocket != null)
         {
            controlSocket.close();
         }
         
         log.debug(this + " replacing control socket: " + controlSocket);
         controlSocket = socket;
         log.debug(this + " control socket replaced by: " + socket);
         controlOutputStream = controlSocket.getOutputStream();
         log.debug("controlOutputStream replaced by: " + controlOutputStream);
      }

      if (pingTimerTask != null)
         pingTimerTask.cancel();

      if (pingFrequency > 0)
      {
         pingTimerTask = new PingTimerTask(this);

         synchronized (timerLock)
         {
            if (timer == null)
            {
               timer = new Timer(true);
            }
            try
            {
               timer.schedule(pingTimerTask, pingFrequency, pingFrequency);
            }
            catch (IllegalStateException e)
            {
               log.debug("Unable to schedule TimerTask on existing Timer", e);
               timer = new Timer(true);
               timer.schedule(pingTimerTask, pingFrequency, pingFrequency);
            }
         }
      }
   }


   InvokerLocator getSecondaryLocator() throws Throwable
   {
      InternalInvocation ii = new InternalInvocation(Bisocket.GET_SECONDARY_INVOKER_LOCATOR, null);
      InvocationRequest r = new InvocationRequest(null, null, ii, null, null, null);
      log.debug("getting secondary locator");
      Exception savedException = null;
      
      for (int i = 0; i < maxRetries; i++)
      {
         try
         {
            Object o = invoke(r);
            log.debug("secondary locator: " + o);
            return (InvokerLocator) o;
         }
         catch (Exception e)
         {
            savedException = e;
            log.debug("unable to get secondary locator: trying again");
         }
      }
      
      throw savedException;
   }


   public InvokerLocator getCallbackLocator(Map metadata)
   {
      String transport = (String) metadata.get(Client.CALLBACK_SERVER_PROTOCOL);
      String host = (String) metadata.get(Client.CALLBACK_SERVER_HOST);
      String sPort = (String) metadata.get(Client.CALLBACK_SERVER_PORT);
      int port = -1;
      if (sPort != null)
      {
         try
         {
            port = Integer.parseInt(sPort);
         }
         catch (NumberFormatException e)
         {
            throw new RuntimeException("Can not set internal callback server port as configuration value (" + sPort + " is not a number.");
         }
      }

      return new InvokerLocator(transport, host, port, "callback", metadata);
   }


   static class PingTimerTask extends TimerTask
   {
      private Object controlLock;
      private OutputStream controlOutputStream;
      private int maxRetries;
      private Exception savedException;
      private boolean running = true;
      private boolean pingSent;
      private BooleanHolder pingFailed;
      
      PingTimerTask(BisocketClientInvoker invoker)
      {
         controlLock = invoker.controlLock;
         controlOutputStream = invoker.controlOutputStream;
         maxRetries = invoker.getMaxRetries();
         pingFailed = invoker.pingFailed;
         pingFailed.flag = false;
      }
      
      public void shutDown()
      {
         synchronized (controlLock)
         {
            controlOutputStream = null;
         }
         cancel();
         
         try
         {
            Method purge = getDeclaredMethod(Timer.class, "purge", new Class[]{});
            purge.invoke(timer, new Object[]{});
         }
         catch (Exception e)
         {
            log.debug("running with jdk 1.4: unable to purge Timer");
         }
      }

      public void run()
      {     
         pingSent = false;
         
         for (int i = 0; i < maxRetries; i++)
         {
            try
            {
               synchronized (controlLock)
               {
                  if (!running)
                     return;
                     
                  controlOutputStream.write(Bisocket.PING);
               }
               pingSent = true;
               break;
            }
            catch (Exception e)
            {
               savedException = e;
               log.debug("Unable to send ping: trying again");
            }
         }
         
         if (!running)
            return;
         
         if (!pingSent)
         {
            log.warn("Unable to send ping: shutting down PingTimerTask", savedException);
            pingFailed.flag = true;
            shutDown();  
         }
      }
   }
   
   static class BooleanHolder
   {
      public boolean flag;
      
      public BooleanHolder(boolean flag)
      {
         this.flag = flag;
      }
   }
   
   static private Method getDeclaredMethod(final Class c, final String name, final Class[] parameterTypes)
   throws NoSuchMethodException
   {
      if (SecurityUtility.skipAccessControl())
      {
         Method m = c.getDeclaredMethod(name, parameterTypes);
         m.setAccessible(true);
         return m;
      }

      try
      {
         return (Method) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws NoSuchMethodException
            {
               Method m = c.getDeclaredMethod(name, parameterTypes);
               m.setAccessible(true);
               return m;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (NoSuchMethodException) e.getCause();
      }
   }
}