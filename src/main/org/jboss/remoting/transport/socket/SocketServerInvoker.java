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

package org.jboss.remoting.transport.socket;

import org.jboss.remoting.Home;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.util.SecurityUtility;
import org.jboss.remoting.util.TimerUtil;
import org.jboss.remoting.marshal.serializable.SerializableMarshaller;
import org.jboss.util.propertyeditor.PropertyEditors;
import org.jboss.logging.Logger;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLException;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimerTask;

/**
 * SocketServerInvoker is the server-side of a SOCKET based transport
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 *
 * @version $Revision: 5082 $
 * @jmx:mbean
 */
public class SocketServerInvoker extends ServerInvoker implements SocketServerInvokerMBean
{
   private static final Logger log = Logger.getLogger(SocketServerInvoker.class);

   private static boolean trace = log.isTraceEnabled();

   static int clientCount = 0;

   protected Properties props = new Properties();

   private static int BACKLOG_DEFAULT = 200;
   protected static int MAX_POOL_SIZE_DEFAULT = 300;

   /**
    * Key for indicating if socket invoker should continue to keep socket connection between
    * client and server open after invocations by sending a ping on the connection
    * before being re-used.  The default for this is false.
    */
   public static final String CHECK_CONNECTION_KEY = "socket.check_connection";

   /**
    * Specifies the fully qualified class name for the custom SocketWrapper implementation to use on the server.
    */
   public static final String SERVER_SOCKET_CLASS_FLAG = "serverSocketClass";
   protected String serverSocketClass = ServerSocketWrapper.class.getName();

   protected List serverSockets = new ArrayList();
   protected boolean running = false;
   protected int backlog = BACKLOG_DEFAULT;
   protected AcceptThread[] acceptThreads;
   protected int numAcceptThreads = 1;
   protected int maxPoolSize = MAX_POOL_SIZE_DEFAULT;
   protected LRUPool clientpool;
   protected LinkedList threadpool;
   protected boolean immediateShutdown;

   protected ServerSocketRefresh refreshThread;
   protected boolean newServerSocketFactory = false;
   protected Object serverSocketFactoryLock = new Object();

   protected boolean reuseAddress = true;
   protected int receiveBufferSize = -1;
   
   /**
    * More socket configuration parameters.
    */
   protected boolean keepAlive;
   protected boolean keepAliveSet;
   protected boolean oOBInline;
   protected boolean oOBInlineSet;
   protected int sendBufferSize = -1;
   protected boolean soLinger;
   protected boolean soLingerSet;
   protected int soLingerDuration = -1;
   protected int trafficClass = -1;

   // defaults to -1 as to not have idle timeouts
   protected int idleTimeout = -1;
   protected IdleTimerTask idleTimerTask = null;
   
   protected int writeTimeout = -1;

   public SocketServerInvoker(InvokerLocator locator)
   {
      super(locator);
   }

   public SocketServerInvoker(InvokerLocator locator, Map configuration)
   {
      super(locator, configuration);
   }

   /**
    * after a truststore update use this to
    * set a new ServerSocketFactory to the invoker<br>
    * then a new ServerSocket is created that accepts the new connections
    * @param serverSocketFactory
 	*/
   public void setNewServerSocketFactory(ServerSocketFactory serverSocketFactory)
   {
      log.trace("entering setNewServerSocketFactory()");
      synchronized (serverSocketFactoryLock)
      {
         newServerSocketFactory=true;
         setServerSocketFactory(serverSocketFactory);
         serverSocketFactoryLock.notify();
         log.info("ServerSocketFactory has been updated");
      }
   }

   /**
    * refreshes the serverSocket by closing old one and
    * creating a new ServerSocket from new ServerSocketFactory
    * @throws IOException
    */
   protected void refreshServerSocket() throws IOException
   {
      log.trace("entering refreshServerSocket()");
      synchronized (serverSocketFactoryLock)
      {
         for (int i = 0; i < acceptThreads.length; i++)
         {
            // If release() is able to enter its synchronized block and sees 
            // serverSocket == null, then it knows that something went wrong.
            newServerSocketFactory=false;
            ServerSocket oldServerSocket = acceptThreads[i].getServerSocket();
            InetAddress address = oldServerSocket.getInetAddress();
            int port = oldServerSocket.getLocalPort();
            oldServerSocket.close();
            ServerSocket newServerSocket = null;
            
            for (int j = 0; j < 5; j++)
            {
               try
               {
                  newServerSocket = createServerSocket(port, backlog, address);
                  break;
               }
               catch (Exception e)
               {
                  if (j < 4)
                  {
                     // Wait for end of TIME_WAIT state (1 to 4 minutes).
                     log.warn("Unable to recreate ServerSocket: will try again in 65 seconds", e);
                     try {Thread.sleep(65000);} catch (InterruptedException ignored) {}
                  }
                  else
                  {
                     log.error("Unable to recreate ServerSocket after 260 seconds", e);
                     return;
                  }
               }
            }
            
            acceptThreads[i].setServerSocket(newServerSocket);
            log.info(acceptThreads[i] + " has been updated with new ServerSocket");
         }
      }
      log.trace("leaving refreshServerSocket()");
   }

   protected void setup() throws Exception
   {
      props.putAll(getConfiguration());
      mapJavaBeanProperties(this, props, false);
      super.setup();
      String ssclass = props.getProperty(SERVER_SOCKET_CLASS_FLAG);
      if(ssclass != null)
      {
         serverSocketClass = ssclass;
      }
   }

   protected void finalize() throws Throwable
   {
      stop();
      super.finalize();
   }

   /**
    * Starts the invoker.
    *
    * @jmx.managed-operation description = "Start sets up the ServerInvoker we are wrapping."
    * impact      = "ACTION"
    */
   public synchronized void start() throws IOException
   {
      if(!running)
      {
         log.debug(this + " starting");

         if(maxPoolSize <= 0)
         {
            //need to reset to default
            maxPoolSize = MAX_POOL_SIZE_DEFAULT;
         }

         clientpool = new LRUPool(2, maxPoolSize);
         clientpool.create();
         threadpool = new LinkedList();

         createServerSockets();
         
         refreshThread = new ServerSocketRefresh();
         refreshThread.setDaemon(true);
         refreshThread.start();
         
         acceptThreads = new AcceptThread[numAcceptThreads * getHomes().size()];
         int i = 0;
         Iterator it = serverSockets.iterator();
         while (it.hasNext())
         {
            ServerSocket ss = (ServerSocket) it.next();
            for(int j = 0; j < numAcceptThreads; j++)
            {
               acceptThreads[i++] = new AcceptThread(ss, refreshThread);
            }
         }
      }

      try
      {
         super.start();
      }
      catch(IOException e)
      {
         log.error("Error starting SocketServerInvoker.", e);
         cleanup();
      }
      if(!running)
      {
         running = true;

         for(int i = 0; i < acceptThreads.length; i++)
         {
            acceptThreads[i].start();
         }
      }

      if(idleTimeout > 0)
      {
         if(idleTimerTask != null)
         {
            idleTimerTask.cancel();
         }
         idleTimerTask = new IdleTimerTask();
         TimerUtil.schedule(idleTimerTask, idleTimeout * 1000);
      }
      else
      {
         if(idleTimerTask != null)
         {
            idleTimerTask.cancel();
         }
      }

      log.debug(this + " started");

   }

   protected ServerSocket createServerSocket(int serverBindPort,
                                             final int backlog,
                                             InetAddress bindAddress) throws IOException
   {
      ServerSocketFactory factory = getServerSocketFactory();
      ServerSocket ss = null;
      
      try
      {
         ss = factory.createServerSocket();
      }
      catch (SocketException e)
      {
         if (getReuseAddress())
            log.warn("Unable to create unbound ServerSocket: cannot set reuseAddress to true",e);
 
         ss = factory.createServerSocket(serverBindPort, backlog, bindAddress);
         configureServerSocket(ss);
         return ss;
      }
      
      ss.setReuseAddress(getReuseAddress());
      configureServerSocket(ss);
      InetSocketAddress address = new InetSocketAddress(bindAddress, serverBindPort);
      bind(ss, address, backlog);
      return ss;
   }
   
   protected void createServerSockets() throws IOException
   {
      ServerSocketFactory factory = getServerSocketFactory();

      Iterator it = getHomes().iterator();
      while (it.hasNext())
      {
         Home home = (Home) it.next();
         InetAddress inetAddress = getAddressByName(home.host);
         
         ServerSocket ss = null;
         try
         {
            ss = factory.createServerSocket();
            ss.setReuseAddress(getReuseAddress());
            configureServerSocket(ss);
            InetSocketAddress address = new InetSocketAddress(inetAddress, home.port);
            bind(ss, address, backlog);
            if (log.isDebugEnabled()) log.debug(this + " created " + ss);
         }
         catch (SocketException e)
         {
            if (getReuseAddress())
               log.warn("Unable to create unbound ServerSocket: cannot set reuseAddress to true");

            try
            {
               ss = factory.createServerSocket(home.port, backlog, inetAddress);
               configureServerSocket(ss);
            }
            catch (IOException e2)
            {
               String m = this + " error creating ServerSocket[" + home + "]: " + e2.getMessage();
               IOException e3 = new IOException(m);
               log.debug(m, e3);
               throw e3;
            }
         }
         catch (IOException e)
         {
            String m = this + " error creating ServerSocket[" + home + "]: " + e.getMessage();
            IOException e2 = new IOException(m);
            log.debug(m, e2);
            throw e2;
         }
         
         serverSockets.add(ss);
      }
   }
   
   protected void configureServerSocket(ServerSocket ss) throws SocketException
   {
      if (receiveBufferSize != -1)
      {
         ss.setReceiveBufferSize(receiveBufferSize);
      }
   }

   protected String getThreadName(int i)
   {
      return "AcceptorThread#" + i + ":" + getServerBindPort();
   }

   public void destroy()
   {
      if(clientpool != null)
      {
         synchronized (clientpool)
         {
            clientpool.destroy();
         }
      }
      super.destroy();
   }

   /**
    * Stops the invoker.
    *
    * @jmx.managed-operation description = "Stops the invoker."
    * impact      = "ACTION"
    */
   public synchronized void stop()
   {
      if(running)
      {
         cleanup();
      }
      super.stop();
   }

   protected void cleanup()
   {
      running = false;
      
      if(acceptThreads != null)
      {
         for(int i = 0; i < acceptThreads.length; i++)
         {
            acceptThreads[i].shutdown();
         }
      }
      
      if (refreshThread != null)
         refreshThread.shutdown();
      
      if (idleTimerTask != null)
      {
         idleTimerTask.cancel();
      }

      maxPoolSize = 0; // so ServerThreads don't reinsert themselves
      
      // The following code has been changed to avoid a race condition with ServerThread.run() which
      // can result in leaving ServerThreads alive, which causes a memory leak.
      if (clientpool != null)
      {
         synchronized (clientpool)
         {
            Set svrThreads = clientpool.getContents();
            Iterator itr = svrThreads.iterator();

            while(itr.hasNext())
            {
               Object o = itr.next();
               ServerThread st = (ServerThread) o;
               if (immediateShutdown)
               {
                  st.shutdownImmediately();
               }
               else
               {
                  st.shutdown();
               }
            }

            clientpool.flush();
            clientpool.stop();
            
            log.debug(this + " stopped threads in clientpool");

            if (threadpool != null)
            {
               int threadsToShutdown = threadpool.size();
               for(int i = 0; i < threadsToShutdown; i++)
               {
                  ServerThread thread = (ServerThread) threadpool.removeFirst();
                  if (immediateShutdown)
                  {
                     thread.shutdownImmediately();
                  }
                  else
                  {
                     thread.shutdown();
                  }
               }
               
               log.debug(this + " stopped threads in threadpool");
            }
         }
      }
      
      log.debug(this + " exiting");
   }


   public int getReceiveBufferSize()
   {
      return receiveBufferSize;
   }

   public void setReceiveBufferSize(int receiveBufferSize)
   {
      this.receiveBufferSize = receiveBufferSize;
   }
   
   /**
    * Indicates if SO_REUSEADDR is enabled on server sockets
    * Default is true.
    */
   public boolean getReuseAddress()
   {
      return reuseAddress;
   }

   /**
    * Sets if SO_REUSEADDR is enabled on server sockets.
    * Default is true.
    *
    * @param reuse
    */
   public void setReuseAddress(boolean reuse)
   {
      this.reuseAddress = reuse;
   }

   public boolean isKeepAlive()
   {
      return keepAlive;
   }

   public void setKeepAlive(boolean keepAlive)
   {
      this.keepAlive = keepAlive;
      keepAliveSet = true;
   }

   public boolean isOOBInline()
   {
      return oOBInline;
   }

   public void setOOBInline(boolean inline)
   {
      oOBInline = inline;
      oOBInlineSet = true;
   }

   public int getSendBufferSize()
   {
      return sendBufferSize;
   }

   public void setSendBufferSize(int sendBufferSize)
   {
      this.sendBufferSize = sendBufferSize;
   }

   public boolean isSoLinger()
   {
      return soLinger;
   }
   
   public int getSoLingerDuration()
   {
      return soLingerDuration;
   }

   public void setSoLinger(boolean soLinger)
   {
      this.soLinger = soLinger;
      soLingerSet = true;
   }

   public void setSoLingerDuration(int soLingerDuration)
   {
      this.soLingerDuration = soLingerDuration;
   }

   public int getTrafficClass()
   {
      return trafficClass;
   }

   public void setTrafficClass(int trafficClass)
   {
      this.trafficClass = trafficClass;
   }
   
   /**
    * @return Number of idle ServerThreads
    * @jmx:managed-attribute
    */
   public int getCurrentThreadPoolSize()
   {
      return threadpool.size();
   }

   /**
    * @return Number of ServerThreads current executing or waiting on an invocation
    * @jmx:managed-attribute
    */
   public int getCurrentClientPoolSize()
   {
      return clientpool.size();
   }

   /**
    * Getter for property numAcceptThreads
    *
    * @return The number of threads that exist for accepting client connections
    * @jmx:managed-attribute
    */
   public int getNumAcceptThreads()
   {
      return numAcceptThreads;
   }

   /**
    * Setter for property numAcceptThreads
    *
    * @param size The number of threads that exist for accepting client connections
    * @jmx:managed-attribute
    */
   public void setNumAcceptThreads(int size)
   {
      this.numAcceptThreads = size;
   }

   /**
    * Setter for max pool size.
    * The number of server threads for processing client. The default is 300.
    *
    * @return
    * @jmx:managed-attribute
    */
   public int getMaxPoolSize()
   {
      return maxPoolSize;
   }

   /**
    * The number of server threads for processing client. The default is 300.
    *
    * @param maxPoolSize
    * @jmx:managed-attribute
    */
   public void setMaxPoolSize(int maxPoolSize)
   {
      this.maxPoolSize = maxPoolSize;
   }

   /**
    * @jmx:managed-attribute
    */
   public int getBacklog()
   {
      return backlog;
   }

   /**
    * @jmx:managed-attribute
    */
   public void setBacklog(int backlog)
   {
      if(backlog < 0)
      {
         this.backlog = BACKLOG_DEFAULT;
      }
      else
      {
         this.backlog = backlog;
      }
   }

   public int getIdleTimeout()
   {
      return idleTimeout;
   }

   /**
    * Sets the timeout for idle threads to be removed from pool.
    * If the value is greater than 0, then idle timeout will be
    * activated, otherwise no idle timeouts will occur.  By default,
    * this value is -1.
    *
    * @param idleTimeout number of seconds before a idle thread is timed out.
    */
   public void setIdleTimeout(int idleTimeout)
   {
      this.idleTimeout = idleTimeout;

      if(isStarted())
      {
         if(idleTimeout > 0)
         {
            if(idleTimerTask != null)
            {
               idleTimerTask.cancel();
            }
            idleTimerTask = new IdleTimerTask();
            TimerUtil.schedule(idleTimerTask, idleTimeout * 1000);
         }
         else
         {
            if(idleTimerTask != null)
            {
               idleTimerTask.cancel();
            }
         }
      }
   }

   public boolean isImmediateShutdown()
   {
      return immediateShutdown;
   }

   public void setImmediateShutdown(boolean immediateShutdown)
   {
      this.immediateShutdown = immediateShutdown;
   }

   public int getWriteTimeout()
   {
      return writeTimeout;
   }

   public void setWriteTimeout(int writeTimeout)
   {
      this.writeTimeout = writeTimeout;
   }

   protected void configureSocket(Socket s) throws SocketException
   {
      s.setReuseAddress(getReuseAddress());
      
      if (keepAliveSet)           s.setKeepAlive(keepAlive);
      if (oOBInlineSet)           s.setOOBInline(oOBInline);
      if (receiveBufferSize > -1) s.setReceiveBufferSize(receiveBufferSize);
      if (sendBufferSize > -1)    s.setSendBufferSize(sendBufferSize);
      if (soLingerSet && 
            soLingerDuration > 0) s.setSoLinger(soLinger, soLingerDuration);
      if (trafficClass > -1)      s.setTrafficClass(trafficClass);
   }
   
   /**
    * The acceptor thread should spend as little time as possbile doing any kind of operation, and
    * under no circumstances should perform IO on the new socket, which can potentially block and
    * lock up the server. For this reason, the acceptor thread should grab a worker thread and
    * delegate all subsequent work to it.
    */
   protected void processInvocation(Socket socket) throws Exception
   {
      ServerThread worker = null;
      boolean newThread = false;

      synchronized(clientpool)
      {
         while(worker == null && running)
         {
            if(trace) { log.trace(this + " trying to get a worker thread from threadpool for processing"); }

            if(threadpool.size() > 0)
            {
               worker = (ServerThread)threadpool.removeFirst();
               if(trace) { log.trace(this + (worker == null ? " found NO threads in threadpool"
                                                            : " got " + worker + " from threadpool")); }
               
            }
            else if (trace) { { log.trace(this + " has an empty threadpool"); } }

            if(worker == null)
            {
               if(clientpool.size() < maxPoolSize)
               {
                  if(trace) { log.trace(this + " creating new worker thread"); }
                  worker = new ServerThread(socket, this, clientpool, threadpool,
                                            getTimeout(), writeTimeout, serverSocketClass);
                  if(trace) { log.trace(this + " created " + worker); }
                  newThread = true;
               }

               if(worker == null)
               {
                  if(trace) {log.trace(this + " trying to evict a thread from clientpool"); }
                  clientpool.evict();
                  clientpool.wait(1000);  // Keep trying, in case all threads are not evictable.
                  if(trace) { log.trace(this + " notified of clientpool thread availability"); }
               }
            }
         }

         if (!running)
         {
            return;
         }
         clientpool.insert(worker, worker);
      }

      if(newThread)
      {
         if(trace) {log.trace(this + " starting " + worker); }
         worker.start();
      }
      else
      {
         if(trace) { log.trace(this + " reusing " + worker); }
         worker.wakeup(socket, getTimeout(), this);
      }
   }

   /**
    * returns true if the transport is bi-directional in nature, for example,
    * SOAP in unidirectional and SOCKETs are bi-directional (unless behind a firewall
    * for example).
    */
   public boolean isTransportBiDirectional()
   {
      return true;
   }

   public String toString()
   {
      return "SocketServerInvoker[" + locator.getHomes() + "]";
   }

   /**
    * Each implementation of the remote client invoker should have
    * a default data type that is uses in the case it is not specified
    * in the invoker locator uri.
    */
   protected String getDefaultDataType()
   {
      return SerializableMarshaller.DATATYPE;
   }

   /**
    * this thread checks if a new ServerSocketFactory was set,<br>
    * if so initializes a serversocket refresh
    * @author Michael Voss
    *
    */
   public class ServerSocketRefresh extends Thread
   {  
      private boolean running = true;
      
      public ServerSocketRefresh()
      {
         super("ServerSocketRefresh");
      }
      
      public void run()
      {
         while(running)
         {
            synchronized (serverSocketFactoryLock)
            {  
               if(newServerSocketFactory)
               {
                  log.debug("got notice about new ServerSocketFactory");
                  try
                  {
                     log.debug("refreshing server socket");
                     refreshServerSocket();
                     log.debug("server socket refreshed");
                  } catch (IOException e)
                  {
                     log.error("could not refresh server socket", e);
                  }
               }
               
               try
               {
                  serverSocketFactoryLock.wait();
                  log.trace("ServerSocketRefresh thread woke up");
               }
               catch (InterruptedException e)
               {
               }
            }
         }
         log.debug("ServerSocketRefresh shutting down");
      }

      /**
       * Let SocketServerInvoker.run() resume when refresh is completed
       */
      public void release() throws InvalidStateException
      {
         synchronized (serverSocketFactoryLock)
         {
//            if (serverSocket == null)
//            {
//               throw new InvalidStateException("error refreshing ServerSocket");
//            }
            log.trace("passed through ServerSocketRefresh.release()");
         }
      }
      
      public void shutdown()
      {
         running = false;
         
         synchronized (serverSocketFactoryLock)
         {
            serverSocketFactoryLock.notify();
         }
      }
   }

   /**
    * The IdleTimerTask is used to periodically check the server threads to
    * see if any have been idle for a specified amount of time, and if so,
    * release those threads and their connections and clear from the server
    * thread pool.
    */
   public class IdleTimerTask extends TimerTask
   {
      public void run()
      {
         Object[] svrThreadArray = null;
         Set threadsToShutdown = new HashSet();

         synchronized(clientpool)
         {
            Set svrThreads = clientpool.getContents();
            svrThreadArray = svrThreads.toArray();

            if(trace)
            {
               if(svrThreadArray != null)
               {
                  log.trace("Idle timer task fired.  Number of ServerThreads = " + svrThreadArray.length);
               }
            }

            // iterate through pooled server threads and evict idle ones
            if(svrThreadArray != null)
            {
               long currentTime = System.currentTimeMillis();

               for(int x = 0; x < svrThreadArray.length; x++)
               {
                  ServerThread svrThread = (ServerThread)svrThreadArray[x];

                  // check the idle time and evict
                  long idleTime = currentTime - svrThread.getLastRequestTimestamp();

                  if(trace)
                  {
                     log.trace("Idle time for ServerThread (" + svrThread + ") is " + idleTime);
                  }

                  long idleTimeout = getIdleTimeout() * 1000;
                  if(idleTime > idleTimeout)
                  {
                     if(trace)
                     {
                        log.trace("Idle timeout reached for ServerThread (" + svrThread + ") and will be evicted.");
                     }
                     clientpool.remove(svrThread);
                     threadsToShutdown.add(svrThread);
//                     svrThread.shutdown();
//                     svrThread.unblock();
                  }
               }
            }

            // now check idle server threads in the thread pool
            svrThreadArray = null;

            if(threadpool.size() > 0)
            {
               // now need to check the tread pool to remove threads
               svrThreadArray = threadpool.toArray();
            }

            if(trace)
            {
               if(svrThreadArray != null)
               {
                  log.trace("Number of ServerThread in thead pool = " + svrThreadArray.length);
               }
            }

            if(svrThreadArray != null)
            {
               long currentTime = System.currentTimeMillis();

               for(int x = 0; x < svrThreadArray.length; x++)
               {
                  ServerThread svrThread = (ServerThread)svrThreadArray[x];
                  long idleTime = currentTime - svrThread.getLastRequestTimestamp();

                  if(trace)
                  {
                     log.trace("Idle time for ServerThread (" + svrThread + ") is " + idleTime);
                  }

                  long idleTimeout = getIdleTimeout() * 1000;
                  if(idleTime > idleTimeout)
                  {
                     if(trace)
                     {
                        log.trace("Idle timeout reached for ServerThread (" + svrThread + ") and will be removed from thread pool.");
                     }
                     threadpool.remove(svrThread);
                     threadsToShutdown.add(svrThread);
//                     svrThread.shutdown();
                  }
               }
            }
         }
         
         Iterator it = threadsToShutdown.iterator();
         while (it.hasNext())
         {
            ServerThread svrThread = (ServerThread) it.next();
            svrThread.shutdown();
//            svrThread.unblock();
         }
      }
   }
   
   public class AcceptThread extends Thread
   {
      ServerSocket serverSocket;
      ServerSocketRefresh refreshThread; 
      
      public AcceptThread(ServerSocket serverSocket, ServerSocketRefresh refreshThread)
      {
         this.serverSocket = serverSocket;
         this.refreshThread = refreshThread;
         setName("AcceptorThread[" + serverSocket + "]");
         if(trace) log.trace(SocketServerInvoker.this + " created " + this); 
      }
      
      public void run()
      {
         if(trace) { log.trace(this + " started execution of method run()"); }

         while(running)
         {
            try
            {
               refreshThread.release(); //goes on if serversocket refresh is completed

               if(trace) { log.trace(this + " is going to wait on serverSocket.accept()"); }

               Socket socket = accept(serverSocket);
               if(trace) { log.trace(this + " accepted " + socket); }

               // the acceptor thread should spend as little time as possbile doing any kind of
               // operation, and under no circumstances should perform IO on the new socket, which
               // can potentially block and lock up the server. For this reason, the acceptor thread
               // should grab a worker thread and delegate all subsequent work to it. This is what
               // processInvocation() does.

               configureSocket(socket);
               processInvocation(socket);
            }
            catch (SSLException e)
            {
               log.error("SSLServerSocket error", e);
               return;
            }
            catch (InvalidStateException e)
            {
               log.error("Cannot proceed without functioning server socket.  Shutting down", e);
               return;
            }
            catch(Throwable ex)
            {  
               if(running)
               {
                  log.error(this + " failed to handle socket", ex);
               }
               else
               {
                  log.trace(this + " caught exception in run()", ex);     
               }
            }
         }
      }
      
      public  void shutdown()
      {
         try
         {
            serverSocket.close();
         }
         catch (IOException e)
         {
            log.debug(this + " error closing " + serverSocket, e);
         }
      }
      
      public ServerSocket getServerSocket()
      {
         return serverSocket;
      }
      
      public void setServerSocket(ServerSocket serverSocket)
      {
         this.serverSocket = serverSocket;
      }
   }
   
   static private void mapJavaBeanProperties(final Object o, final Properties props, final boolean isStrict)
   throws IntrospectionException
   {
      if (SecurityUtility.skipAccessControl())
      {
         PropertyEditors.mapJavaBeanProperties(o, props, isStrict);
         return;
      }

      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IntrospectionException
            {
               PropertyEditors.mapJavaBeanProperties(o, props, isStrict);
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (IntrospectionException) e.getCause();
      }
   }
   
   static private Socket accept(final ServerSocket ss) throws IOException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return ss.accept();
      }
      
      try
      {
          return (Socket)AccessController.doPrivileged( new PrivilegedExceptionAction()
          {
             public Object run() throws Exception
             {
                 return ss.accept();
             }
          });
      }
      catch (PrivilegedActionException e)
      {
          throw (IOException) e.getCause();
      }
   }

   static private void bind(final ServerSocket ss, final SocketAddress address,
                           final int backlog) throws IOException
   {
      if (SecurityUtility.skipAccessControl())
      {
         ss.bind(address, backlog);
         return;
      }
      
      try
      {
          AccessController.doPrivileged( new PrivilegedExceptionAction()
          {
             public Object run() throws Exception
             {
                ss.bind(address, backlog);
                return null;
             }
          });
      }
      catch (PrivilegedActionException e)
      {
          throw (IOException) e.getCause();
      }
   }
   
   static private InetAddress getAddressByName(final String host) throws UnknownHostException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return InetAddress.getByName(host);
      }
      
      try
      {
         return (InetAddress)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IOException
            {
               return InetAddress.getByName(host);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (UnknownHostException) e.getCause();
      }
   }
}
