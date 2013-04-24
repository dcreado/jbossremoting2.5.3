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
package org.jboss.remoting.detection;


import org.jboss.logging.Logger;
import org.jboss.remoting.ConnectionValidator;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.InvokerRegistry;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.ident.Identity;
import org.jboss.remoting.network.NetworkInstance;
import org.jboss.remoting.network.NetworkRegistryFinder;
import org.jboss.remoting.network.NetworkRegistryMBean;
import org.jboss.remoting.network.NetworkRegistryWrapper;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;


/**
 * AbstractDetector
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 * @version $Revision: 5116 $
 */
public abstract class AbstractDetector implements AbstractDetectorMBean
{
   static protected final Logger log = Logger.getLogger(AbstractDetector.class);
   
   private long defaultTimeDelay = 5000;
   private long heartbeatTimeDelay = 1000;
   protected MBeanServer mbeanserver;
   protected ObjectName objectName;
   protected ObjectName registryObjectName;
   protected NetworkRegistryMBean networkRegistry;

   private Identity myself;
   private Timer heartbeatTimer;
   private Timer failureTimer;
   private Map servers = new HashMap();
   private Element xml;
   private Set domains = new HashSet();
   private boolean acceptLocal = false;

   private Map config;

   public AbstractDetector()
   {
      this(null);
   }

   public AbstractDetector(Map config)
   {
      this.config = new HashMap();
      if (config != null)
         this.config.putAll(config);
   }

   /**
    * The amount of time to wait between sending (and sometimes receiving) detection messages.
    *
    * @param heartbeatTimeDelay
    * @throws IllegalArgumentException
    */
   public void setHeartbeatTimeDelay(long heartbeatTimeDelay)
   {
      if(heartbeatTimeDelay > 0 && heartbeatTimeDelay < defaultTimeDelay)
      {
         this.heartbeatTimeDelay = heartbeatTimeDelay;
      }
      else
      {
         throw new IllegalArgumentException("Can not set heartbeat time delay (" + heartbeatTimeDelay + ") to a negative number or " +
                                            "to a number greater than the default time delay (" + defaultTimeDelay + ").");
      }
   }

   /**
    * The amount of time to wait between sending (and sometimes receiving) detection messages.
    *
    * @return
    */
   public long getHeartbeatTimeDelay()
   {
      return heartbeatTimeDelay;
   }

   /**
    * The amount of time which can elapse without receiving a detection event before a server
    * will be suspected as being dead and peroforming an explicit invocation on it to verify it is alive.
    *
    * @param defaultTimeDelay time in milliseconds
    * @throws IllegalArgumentException
    */
   public void setDefaultTimeDelay(long defaultTimeDelay)
   {
      if(defaultTimeDelay >= heartbeatTimeDelay)
      {
         this.defaultTimeDelay = defaultTimeDelay;
      }
      else
      {
         throw new IllegalArgumentException("Can not set the default time delay (" + defaultTimeDelay + ") to be less" +
                                            " than that of the heartbeat time delay (" + heartbeatTimeDelay + ").");
      }
   }

   /**
    * @return The amount of time which can elapse without receiving a detection event before a server
    *         will be suspected as being dead and peroforming an explicit invocation on it to verify it is alive.
    */
   public long getDefaultTimeDelay()
   {
      return defaultTimeDelay;
   }

   /**
    * Will create a detection message based on the server invokers registered within the local InvokerRegistry.
    * The detection message will contain the identity and array of server invoker metadata.
    *
    * @return
    */
   public Detection createDetection()
   {
      Detection detection = null;

      ServerInvoker invokers[] = InvokerRegistry.getServerInvokers();
      if(invokers == null || invokers.length <= 0)
      {
         return detection;
      }
      List l = new ArrayList(invokers.length);
      for(int c = 0; c < invokers.length; c++)
      {
         if(invokers[c].isStarted())
         {
            ServerInvokerMetadata serverInvoker = new ServerInvokerMetadata(invokers[c].getLocator(),
                                                                            invokers[c].getSupportedSubsystems());
            l.add(serverInvoker);
         }
      }
      if(l.isEmpty())
      {
         return detection;
      }
      ServerInvokerMetadata metadata[] = (ServerInvokerMetadata[]) l.toArray(new ServerInvokerMetadata[l.size()]);
      detection = new Detection(Identity.get(mbeanserver), metadata);
      return detection;
   }

   /**
    * called by MBeanServer to start the mbean lifecycle
    *
    * @throws Exception
    */
   public void start() throws Exception
   {
      // get our own identity
      myself = Identity.get(mbeanserver);

      // add my domain if domains empty and xml not set
      if(domains.isEmpty() && xml == null)
      {
         domains.add(myself.getDomain());
      }

      // find our NetworkRegistry
      registryObjectName = NetworkRegistryFinder.find(mbeanserver);
      if(registryObjectName == null)
      {
         log.warn("Detector: " + getClass().getName() + " could not be loaded because the NetworkRegistry is not registered");
         log.warn("This means that only the broadcasting of detection messages will be functional and will not be able to discover other servers.");
      }
      else
      {
         Object o = MBeanServerInvocationHandler.newProxyInstance(mbeanserver,
                                                                  registryObjectName,
                                                                  NetworkRegistryMBean.class,
                                                                  false);                                                       
         networkRegistry = new NetworkRegistryWrapper((NetworkRegistryMBean) o);
      }

      startPinger(getPingerDelay(), getPingerPeriod());
      startHeartbeat(getHeartbeatDelay(), getHeartbeatPeriod());
   }

   /**
    * return the delay in milliseconds between when the timer is created to when the first pinger thread runs.
    * defaults to <tt>5000</tt>
    *
    * @return
    */
   protected long getPingerDelay()
   {
      return 5000;
   }

   /**
    * return the period in milliseconds between checking lost servers against the last detection timestamp.
    * defaults to <tt>1500</tt>
    *
    * @return
    */
   protected long getPingerPeriod()
   {
      return 1500;
   }

   /**
    * start the pinger timer thread
    *
    * @param delay
    * @param period
    */
   protected void startPinger(long delay, long period)
   {
      failureTimer = new Timer(false);
      failureTimer.schedule(new FailureDetector(), delay, period);
   }

   /**
    * stop the pinger timer thread
    */
   protected void stopPinger()
   {
      if(failureTimer != null)
      {
         failureTimer.cancel();
         failureTimer = null;
      }
   }

   /**
    * called by the MBeanServer to stop the mbean lifecycle
    *
    * @throws Exception
    */
   public void stop() throws Exception
   {
      stopPinger();
      stopHeartbeat();
      stopPinger();
   }

   public void postDeregister()
   {
   }

   public void postRegister(Boolean aBoolean)
   {
   }

   public void preDeregister() throws Exception
   {
   }

   public ObjectName preRegister(MBeanServer mBeanServer, ObjectName objectName) throws Exception
   {
      this.mbeanserver = mBeanServer;
      this.objectName = objectName;
      return objectName;
   }

   /**
    * set the configuration for the domains to be recognized by detector
    *
    * @param xml
    * @jmx.managed-attribute description="Configuration is an xml element indicating domains to be recognized by detector"
    * access="read-write"
    */
   public void setConfiguration(Element xml)
         throws Exception
   {
      this.xml = xml;

      // check configuration xml
      if(xml != null)
      {
         // clearing collection of domains since have new ones to set
         domains.clear();

         NodeList domainNodes = xml.getElementsByTagName("domain");
         if(domainNodes == null || domainNodes.getLength() <= 0)
         {
            // no domains specified, so will accept all domains
            log.debug("No domains specified.  Will accept all domains.");
         }
         int len = domainNodes.getLength();
         for(int c = 0; c < len; c++)
         {
            Node node = domainNodes.item(c);
            String domain = node.getFirstChild().getNodeValue();
            domains.add(domain);
            log.debug("Added domain " + domain + " to detector list.");
         }

         // now look to see if local server detection should be accepted
         NodeList localNode = xml.getElementsByTagName("local");
         if(localNode != null)
         {
            acceptLocal = true;
         }


      }
   }

   /**
    * The <code>getConfiguration</code> method
    *
    * @return an <code>Element</code> value
    * @jmx.managed-attribute
    */
   public Element getConfiguration()
   {
      return xml;
   }

   //----------------------- protected

   /**
    * start heartbeating
    *
    * @param delay
    * @param period
    */
   protected void startHeartbeat(long delay, long period)
   {
      if(heartbeatTimer == null)
      {
         heartbeatTimer = new Timer(false);
      }

      try
      {
         heartbeatTimer.schedule(new Heartbeat(), delay, period);
      }
      catch (IllegalStateException e)
      {
         log.debug("Unable to schedule TimerTask on existing Timer", e);
         heartbeatTimer = new Timer(false);
         heartbeatTimer.schedule(new Heartbeat(), delay, period);
      }
   }

   /**
    * stop heartbeating
    */
   protected void stopHeartbeat()
   {
      if(heartbeatTimer != null)
      {
         try
         {
            heartbeatTimer.cancel();
         }
         catch(Exception eg)
         {
         }
         heartbeatTimer = null;
      }
   }

   /**
    * return the initial delay in milliseconds before the initial heartbeat is fired.
    * Defaults to <tt>0</tt>
    *
    * @return
    */
   protected long getHeartbeatDelay()
   {
      return 0;
   }

   /**
    * return the period in milliseconds between subsequent heartbeats. Defaults to
    * <tt>1000</tt>
    *
    * @return
    */
   protected long getHeartbeatPeriod()
   {
      return heartbeatTimeDelay;
   }

   /**
    * subclasses must implement to provide the specific heartbeat protocol
    * for this server to send out to other servers on the network
    */
   protected abstract void heartbeat();

   /**
    * To be used to force detection to occur in synchronouse manner
    * instead of being passive and waiting for detection messages to
    * come in from remote detectors.  The servers returned should be
    * the remote servers that are online at this point in time.  Note, calling this
    * method may take a few seconds to complete.
    * @return
    */
   public NetworkInstance[] forceDetection()
   {
      forceHeartbeat();
      if(networkRegistry != null)
      {
         return (NetworkInstance[]) AccessController.doPrivileged( new PrivilegedAction()
         {
            public Object run()
            {
               return networkRegistry.getServers();
            }
         });
      }
      else
      {
         return null;
      }
   }

   /**
    * Used to force detection messages to be sent by remoting servers
    * and consumed by detector and registered with network registry.
    */
   protected abstract void forceHeartbeat();


   /**
    * called when a remote detection from a peer is received by a detector
    *
    * @param detection
    */
   protected void detect(final Detection detection)
   {
      if (detection != null)
      {
         if (log.isTraceEnabled())
         {
            log.trace("Detection message received.");
            log.trace("Id = " + detection.getIdentity() != null ? detection.getIdentity().getInstanceId() : "null");
            log.trace("isRemoteDetection() = " + isRemoteDetection(detection));
         }
         // we only track detections within our own domain and not ourself
         if (isRemoteDetection(detection))
         {
            try
            {
               boolean found = false;
               Server server = null;

               synchronized (servers)
               {
                  server = (Server) servers.get(detection);
                  found = server != null;
                  if (!found)
                  {
                     // update either way the timestamp and the detection
                     servers.put(detection, new Server(detection));
                  }
                  else
                  {
                     server.lastDetection = System.currentTimeMillis();
                  }
               }
               if (found == false)
               {
                  if (networkRegistry != null)
                  {
                     log.debug(this + " detected NEW server: " + detection);
                 
                     AccessController.doPrivileged( new PrivilegedAction()
                     {
                        public Object run()
                        {
                           networkRegistry.addServer(detection.getIdentity(), detection.getServerInvokers());
                           return null;
                        }
                     });
                  }
               }
               else
               {
                  if (server.changed(detection))
                  {
                     // update hash
                     servers.put(detection, new Server(detection));
                     if (networkRegistry != null)
                     {
                        if (log.isTraceEnabled())
                        {
                           log.trace(this + " detected UPDATE for server: " + detection);
                        }
                        
                        AccessController.doPrivileged( new PrivilegedAction()
                        {
                           public Object run()
                           {
                              networkRegistry.updateServer(detection.getIdentity(), detection.getServerInvokers());
                              return null;
                           }
                        });
                     }
                  }
               }
            }
            catch (Exception e)
            {
               log.warn("Error during detection of: " + detection);
               log.debug("Error during detection of: " + detection, e);
            }
         }
         else if (log.isTraceEnabled())
         {
            log.trace("detection from myself - ignored");
         }
      }
   }

   protected boolean isRemoteDetection(Detection detection)
   {
      String domain = null;
      if(detection != null)
      {
         Identity identity = detection.getIdentity();
         if(identity != null)
         {
            domain = identity.getDomain();
         }
      }
      // is detection domain in accepted domain collection and not local
      // if domains empty, then accept all
      return (domain == null || domains.isEmpty() || domains.contains(domain)) &&
             (acceptLocal ? true : (myself.isSameJVM(detection.getIdentity()) == false));
   }

   protected boolean checkInvokerServer(final Detection detection, ClassLoader cl)
   {
      boolean ok = false;
      ServerInvokerMetadata[] invokerMetadataArray = detection.getServerInvokers();
      ArrayList validinvokers = new ArrayList();
      for(int c = 0; c < invokerMetadataArray.length; c++)
      {
         InvokerLocator locator = null;
         try
         {
            ServerInvokerMetadata invokerMetadata = invokerMetadataArray[c];
            locator = invokerMetadata.getInvokerLocator();

            boolean isValid = ConnectionValidator.checkConnection(locator, config);
            if(isValid)
            {
               // the transport was successful
               ok = true;
               validinvokers.add(invokerMetadata);
               if(log.isTraceEnabled())
               {
                  log.trace("Successful connection check for " + locator);
               }
            }

         }
         catch(Throwable ig)
         {
            log.debug("failed calling ping on " + detection + " due to " + ig.getMessage());
            if(log.isTraceEnabled())
            {
               log.trace(ig);
            }
         }
      }
      if(ok == false)
      {
         // the server is down!
         // would be nice to also remove from the invoker registry as well, but since
         // don't know all the possible entries for the config map passed when was created,
         // won't be able to identify it.  This means that clients currently using that invoker
         // for the server will have to find out the hard way (by getting exception calling on it).
         try
         {
            if(networkRegistry != null)
            {  
               AccessController.doPrivileged( new PrivilegedAction()
               {
                  public Object run()
                  {
                     networkRegistry.removeServer(detection.getIdentity());
                     return null;
                  }
               });
               
               log.debug("Removed detection " + detection);
            }
         }
         catch(Exception ex)
         {
            log.debug("Error removing server for detection (" + detection + ").  Possible network registry does not exist.");
         }
         finally
         {
            // remove this server, it isn't available any more
            servers.remove(detection);
         }
      }
      else // at least one of the server invokers is still valid
      {
         if(log.isTraceEnabled())
         {
            log.trace("Done checking all locators for suspected dead server.  " +
                      "There are " + validinvokers.size() + " out of original " +
                      invokerMetadataArray.length + " still valid.");
         }
         // need to cause an update to be fired if any server invokers failed
         if(validinvokers.size() != invokerMetadataArray.length)
         {
            ServerInvokerMetadata[] newLocators = (ServerInvokerMetadata[])validinvokers.toArray(new ServerInvokerMetadata[validinvokers.size()]);
            Detection newDetection = new Detection(detection.getIdentity(), newLocators);
            if(log.isTraceEnabled())
            {
               log.trace("Since at least one invoker failed while doing connection check, will be re-evaluating detection for:\n" + newDetection);
            }
            detect(newDetection);
         }
      }

      return ok;
   }


   private final class FailureDetector extends TimerTask
   {
      private int threadCounter = 0;

      public void run()
      {
         Thread.currentThread().setName("Remoting Detector - Failure Detector Thread: " + threadCounter++);

         synchronized (servers)
         {
            if (servers.isEmpty())
            {
               return;
            }
            ClassLoader cl = (ClassLoader) AccessController.doPrivileged( new PrivilegedAction()
            {
               public Object run()
               {
                  return AbstractDetector.class.getClassLoader();
               }
            });
            // walk through each detection and see if it needs checking up on ...
            Collection serverCollection = servers.values();
            Server[] serverArray = (Server[])serverCollection.toArray(new Server[serverCollection.size()]);
            for(int x = 0; x < serverArray.length; x++)
            {
               Server svr = serverArray[x];
               Detection detect = svr.detection;
               long lastDetection = svr.lastDetection;
               long duration = System.currentTimeMillis() - lastDetection;
               if (duration >= defaultTimeDelay)
               {
                  if (log.isTraceEnabled())
                  {
                     log.trace("detection for: " + detect + " has not been received in: " + defaultTimeDelay + " ms, contacting..");
                  }
                  // OK, we've exceeded the time delay since the last time we've detected
                  // this dude, he might be down, let's walk through each of his transports and
                  // see if any of them lead to a valid invocation
                  if (checkInvokerServer(detect, cl))
                  {
                     if (log.isTraceEnabled())
                     {
                        log.trace("detection for: " + detect + " recovered on ping");
                     }
                     svr.lastDetection = System.currentTimeMillis();
                  }
               }
            }
         }
      }

   }

   private final class Server
   {
      Detection detection;
      private int hashCode = 0;
      long lastDetection = System.currentTimeMillis();

      Server(Detection detection)
      {
         this.detection = detection;
         rehash(detection);
      }

      private void rehash(Detection d)
      {
         this.hashCode = hash(d);
      }

      private int hash(Detection d)
      {
         int hc = 0;
         InvokerLocator locators[] = d.getLocators();
         if(locators != null)
         {
            for(int c = 0; c < locators.length; c++)
            {
               hc += locators[c].hashCode();
            }
         }
         return hc;
      }

      boolean changed(Detection detection)
      {
         return hashCode != hash(detection);
      }

      public boolean equals(Object obj)
      {
         return obj instanceof Server && hashCode == obj.hashCode();
      }

      public int hashCode()
      {
         return hashCode;
      }
   }

   private final class Heartbeat extends TimerTask
   {
      private int threadCounter = 0;

      public void run()
      {
         Thread.currentThread().setName("Remoting Detector - Heartbeat Thread: " + threadCounter++);
         heartbeat();
      }
   }
}
