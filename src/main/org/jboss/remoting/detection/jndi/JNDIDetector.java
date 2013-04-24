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
package org.jboss.remoting.detection.jndi;

import org.jboss.logging.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.InvokerRegistry;
import org.jboss.remoting.detection.AbstractDetector;
import org.jboss.remoting.detection.Detection;
import org.jboss.remoting.ident.Identity;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.util.SecurityUtility;
import org.jnp.interfaces.NamingContextFactory;
import org.jnp.server.Main;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import java.util.Properties;

/**
 * This is a remoting detector for the remoting package which uses a JNDI server to
 * maintain the registeries for remote invoker servers (stored as Detection messages).
 * This detector is intended to be used in conjuntion with an external JNDI server that
 * is already running.  This is done by passing all the information needed to connect
 * to the remote JNDI server via the setter methods.  This can also be done within
 * the jboss-service.xml.  An example of the entry is as follows:<p>
 * &lt;mbean code="org.jboss.remoting.detection.jndi.JNDIDetector" name="jboss.remoting:service=Detector,transport=jndi"&gt;<br>
 * &lt;attribute name="Port"&gt;5555&lt;/attribute&gt;<br>
 * &lt;attribute name="Host"&gt;foo.bar.com&lt;/attribute&gt;<br>
 * &lt;attribute name="ContextFactory"&gt;org.jnp.interfaces.NamingContextFactory&lt;/attribute&gt;<br>
 * &lt;attribute name="URLPackage"&gt;org.jboss.naming:org.jnp.interfaces&lt;/attribute&gt;<br>
 * &lt;/mbean&gt;<br><p>
 * Note: The above xml is for the JBoss JNP JNDI server, and has not be tested (just an example).<p>
 * Be aware that just because this detector is stopped (and the entry removed from the JNDI server)
 * remote JNDIDetectors may not recognize that the invoker servers are not available.  This is because
 * once remote invoker servers (connectors) are detected, they will be pinged directly to determine
 * if they are no longer available.  However, no new JNDIDetectors will detect your server once stopped.
 * Also, please note that currently the detection registries are bound at the root context and
 * not a sub context (which is on the todo list, but you know how that goes).<p>
 * Important to also note that if any of the above attributes are set once the detector has
 * started, they will not be used in connecting to the JNDI server until the detector is stopped
 * and re-started (they do not change the JNDI server connection dynamically).<p>
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class JNDIDetector extends AbstractDetector implements JNDIDetectorMBean
{
   private int port;
   private String host;
   private String contextFactory = NamingContextFactory.class.getName();
   ;
   private String urlPackage = "org.jboss.naming:org.jnp.interfaces";
   ;

   private Identity id;
   private Context context;

   public static final String DETECTION_SUBCONTEXT_NAME = "detection";

   private String subContextName = DETECTION_SUBCONTEXT_NAME;

   /**
    * Indicates the number of time will detect before doing check to see if server still alive.
    */
   private int detectionNumber = 5;
   private int cleanDetectionCount = detectionNumber;

   protected final Logger log = Logger.getLogger(getClass());
   
   public JNDIDetector()
   {
   }
   
   public JNDIDetector(Map config)
   {
      super(config);
   }
   
   /**
    * Gets the port used to connect to the JNDI Server.
    *
    * @return
    */
   public int getPort()
   {
      return port;
   }

   /**
    * Sets the port to use when connecting to JNDI server
    *
    * @param port
    */
   public void setPort(int port)
   {
      this.port = port;
   }

   /**
    * Gets the host to use when connecting to JNDI server
    *
    * @return
    */
   public String getHost()
   {
      return host;
   }

   /**
    * Sets the host to use when connecting to JNDI server
    *
    * @param host
    */
   public void setHost(String host)
   {
      this.host = host;
   }

   /**
    * The context factory string used when connecting to the JNDI server
    *
    * @return
    */
   public String getContextFactory()
   {
      return contextFactory;
   }

   /**
    * Sets the sub context name under which detection messages will be bound
    * and looked up.
    * @param subContextName
    */
   public void setSubContextName(String subContextName)
   {
      this.subContextName = subContextName;
   }

   /**
    * Gets the sub context name under which detection messages will be bound and
    * looked up.
    * @return
    */
   public String getSubContextName()
   {
      return this.subContextName;
   }

   /**
    * The context factory string to use when connecting to the JNDI server.
    * Should be a qualified class name for JNDI client.
    *
    * @param contextFactory
    */
   public void setContextFactory(String contextFactory)
   {
      this.contextFactory = contextFactory;
   }

   /**
    * The url package string used when connecting to JNDI server
    *
    * @return
    */
   public String getURLPackage()
   {
      return urlPackage;
   }

   /**
    * The url package string to use when connecting to the JNDI server.
    *
    * @param urlPackage
    */
   public void setURLPackage(String urlPackage)
   {
      this.urlPackage = urlPackage;
   }

   /**
    * Will establish the connection to the JNDI server and start detection of other servers.
    *
    * @throws Exception
    */
   public void start() throws Exception
   {
      createContext();
      id = Identity.get(mbeanserver);
      super.start();
   }

   /**
    * Creates connection to JNDI server (which should have already happened in start()
    * method) and will begin checking for remote servers as well as registering itself
    * so will be visible by remote detectors.
    */
   protected void heartbeat()
   {
      try
      {
         //Need to establish connection to server
         if(context == null)
         {
            createContext();
         }
         checkRemoteDetectionMsg();
      }
      catch(NamingException nex)
      {
         log.error("Can not connect to JNDI server to register local connectors.", nex);
      }
   }

   protected void forceHeartbeat()
   {
      heartbeat();
   }

   /**
    * Gets the number of detection iterations before manually pinging remote
    * server to make sure still alive.
    *
    * @return
    */
   public int getCleanDetectionNumber()
   {
      return detectionNumber;
   }

   /**
    * Sets the number of detection iterations before manually pinging remote
    * server to make sure still alive.  This is needed since remote server
    * could crash and yet still have an entry in the JNDI server, thus
    * making it appear that it is still there.
    *
    * @param cleanDetectionNumber
    */
   public void setCleanDetectionNumber(int cleanDetectionNumber)
   {
      detectionNumber = cleanDetectionNumber;
      cleanDetectionCount = detectionNumber;
   }

   private void checkRemoteDetectionMsg()
   {
      try
      {
         boolean localFound = false;
         cleanDetectionCount++;
         boolean cleanDetect = cleanDetectionCount > detectionNumber;
         String bindName = "";
         NamingEnumeration enumeration = listBindings(context, bindName);
         while(enumeration.hasMore())
         {
            Binding binding = (Binding) enumeration.next();
            Detection regMsg = (Detection) binding.getObject();
            // No need to detect myself here
            if(isRemoteDetection(regMsg))
            {
               log.debug("Detected id: " + regMsg.getIdentity().getInstanceId() + ", message: " + regMsg);

               if(cleanDetect)
               {
                  if(log.isTraceEnabled())
                  {
                     log.trace("Doing clean detection.");
                  }
                  // Need to actually detect if servers registered in JNDI server
                  // are actually there (since could die before unregistering)
                  ClassLoader cl = (ClassLoader) AccessController.doPrivileged( new PrivilegedAction()
                  {
                     public Object run()
                     {
                        return JNDIDetector.class.getClassLoader();
                     }
                  });
                  
                  if(!checkInvokerServer(regMsg, cl))
                  {
                     unregisterDetection(regMsg.getIdentity().getInstanceId());
                  }
                  else
                  {
                     // Now, let parent handle detection
                     detect(regMsg);
                  }
               }
               else
               {
                  // Now, let parent handle detection
                  detect(regMsg);
               }
            }
            else
            {
               //verify local detection message is correct
               if(!verifyLocalDetectionMsg(regMsg))
               {
                  addLocalDetectionMsg();
               }
               localFound = true;
            }
         }
         if(cleanDetect)
         {
            // did clean detect, now need to reset.
            cleanDetectionCount = 0;
         }
         if(!localFound)
         {
            // never found local detection message in list, so add it
            addLocalDetectionMsg();
         }
      }
      catch(NamingException e)
      {
         log.error("Exception getting detection messages from JNDI server.", e);
      }
   }

   private boolean verifyLocalDetectionMsg(Detection regMsg) throws NamingException
   {
      boolean verified = false;

      InvokerLocator[] locators = InvokerRegistry.getRegisteredServerLocators();
      Detection msg = createDetection();
      String sId = id.getInstanceId();
      InvokerLocator[] invokers = regMsg.getLocators();

      // first do sanity check to make sure even local detection msg (just in case)
      if(sId.equals(regMsg.getIdentity().getInstanceId()))
      {

         // now see if invoker list changed
         boolean changed = false;
         if(locators.length != invokers.length)
         {
            changed = true;
         }
         else
         {
            // now need to make sure all the invokers are same now as in old detection msg
            // not the most efficient (or elegant) way to do this, but list is short
            boolean found = false; // flag for if current invoker in list found in old list
            for(int i = 0; i < locators.length; i++)
            {
               found = false;
               for(int x = 0; x < invokers.length; x++)
               {
                  if(locators[i].equals(invokers[x]))
                  {
                     found = true;
                     break;
                  }
               }
               if(!found)
               {
                  break;
               }
            }
            if(!found)
            {
               changed = true;
            }
         }
         if(changed)
         {
            registerDetectionMsg(sId, msg);
         }
         // are sure that local detection is correct in JNDI server now
         verified = true;
      }
      return verified;
   }

   private void addLocalDetectionMsg() throws NamingException
   {
      Detection msg = createDetection();
      String sId = id.getInstanceId();
      registerDetectionMsg(sId, msg);
   }

   private void registerDetectionMsg(String sId, Detection msg) throws NamingException
   {
      if(sId != null && msg != null)
      {
         try
         {
            rebind(context, sId, msg);
            log.info("Added " + sId + " to registry.");
         }
         catch(NameAlreadyBoundException nabex)
         {
            if(log.isTraceEnabled())
            {
               log.trace(sId + " already bound to server.");
            }
         }
      }
   }

   /**
    * Convience method to see if given proper configuration to connect to an
    * existing JNDI server.  If not, will create one via JBoss JNP.  Should
    * really only be needed for testing.
    */
   private void verifyJNDIServer()
   {
      if(host == null || host.length() == 0)
      {
         try
         {
            log.info("JNDI Server configuration information not present so will create a local server.");
           
            Object namingBean = null;
            Class namingBeanImplClass = null;
            try
            {
               namingBeanImplClass = Class.forName("org.jnp.server.NamingBeanImpl");
               namingBean = namingBeanImplClass.newInstance();
               Method startMethod = namingBeanImplClass.getMethod("start", new Class[] {});
               setSystemProperty("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
               startMethod.invoke(namingBean, new Object[] {});
            }
            catch (Exception e)
            {
               log.debug("Cannot find NamingBeanImpl: must be running jdk 1.4");
            }
            
            host = getLocalHostName();
            port = PortUtil.findFreePort(host);

            log.info("Remoting JNDI detector starting JNDI server instance since none where specified via configuration.");
            log.info("Remoting JNDI server started on host + " + host + " and port " + port);

            //If no server information provided, then start one of our own by default
            Main server = new Main();
            if (namingBean != null)
            {
               Class namingBeanClass = Class.forName("org.jnp.server.NamingBean");
               Method setNamingInfoMethod = server.getClass().getMethod("setNamingInfo", new Class[] {namingBeanClass});
               setNamingInfoMethod.invoke(server, new Object[] {namingBean});
            }
            server.setPort(port);
            server.setBindAddress(host);
            server.start();

            contextFactory = NamingContextFactory.class.getName();
            urlPackage = "org.jboss.naming:org.jnp.interfaces";
         }
         catch(Exception e)
         {
            log.error("Error starting up JNDI server since none was specified via configuration.", e);
         }
      }
   }

   /**
    * Will try to establish the initial context to the JNDI server based
    * on the configuration properties set.
    *
    * @throws NamingException
    */
   private void createContext() throws NamingException
   {
      verifyJNDIServer();

      Properties env = new Properties();

      env.put(Context.INITIAL_CONTEXT_FACTORY, contextFactory);
      env.put(Context.PROVIDER_URL, host + ":" + port);
      env.put(Context.URL_PKG_PREFIXES, urlPackage);

      InitialContext initialContext = createContext(env);
      try
      {
         context = initialContextLookup(initialContext, subContextName);
      }
      catch(NamingException e)
      {
         try
         {
            context = createSubcontext(initialContext, subContextName);
         }
         catch(NameAlreadyBoundException e1)
         {
            log.debug("The sub context " + subContextName + " was created before we could.");
            context = initialContextLookup(initialContext, subContextName);
         }
      }
   }

   public void stop() throws Exception
   {
      try
      {
         super.stop();
      }
      finally // Need to cleanup JNDI, even if super's stop throws exception
      {
         String sId = id.getInstanceId();
         try
         {
            unregisterDetection(sId);
         }
         catch(NamingException e)
         {
            log.warn("Could not unregister " + sId + " before shutdown.  " +
                     "Root cause is " + e.getMessage());
         }
      }
   }

   private void unregisterDetection(String sId) throws NamingException
   {
      if(log.isTraceEnabled())
      {
         log.trace("unregistering detector " + sId);
      }
      unbind(context, sId);
   }
   
   static private void setSystemProperty(final String name, final String value)
   {
      if (SecurityUtility.skipAccessControl())
      {
         System.setProperty(name, value);
         return;
      }
      
      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return System.setProperty(name, value);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RuntimeException) e.getCause();
      }
   }
   
   static private InetAddress getLocalHost() throws UnknownHostException
   {
      if (SecurityUtility.skipAccessControl())
      {
         try
         {
            return InetAddress.getLocalHost();
         }
         catch (IOException e)
         {
            return InetAddress.getByName("127.0.0.1");
         }
      }

      try
      {
         return (InetAddress) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IOException
            {
               try
               {
                  return InetAddress.getLocalHost();
               }
               catch (IOException e)
               {
                  return InetAddress.getByName("127.0.0.1");
               }
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (UnknownHostException) e.getCause();
      }
   }
   
   static private String getLocalHostName() throws UnknownHostException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return getLocalHost().getHostName();
      }

      try
      {
         return (String) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IOException
            {
               InetAddress address = null;
               try
               {
                  address = InetAddress.getLocalHost();
               }
               catch (IOException e)
               {
                  address = InetAddress.getByName("127.0.0.1");
               }
               
               return address.getHostName();
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (UnknownHostException) e.getCause();
      }
   }
   
   static private Context createSubcontext(final InitialContext initialContext, final String subContextName)
   throws NamingException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return initialContext.createSubcontext(subContextName);
      }

      try
      {
         return (Context) AccessController.doPrivileged( new PrivilegedExceptionAction() 
         {
            public Object run() throws NamingException
            {
               return initialContext.createSubcontext(subContextName);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (NamingException) e.getCause();
      }
   }
   
   static private Context initialContextLookup(final InitialContext initialContext, final String subContextName)
   throws NamingException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return (Context) initialContext.lookup(subContextName);
      }

      try
      {
         return (Context) AccessController.doPrivileged( new PrivilegedExceptionAction() 
         {
            public Object run() throws NamingException
            {
               return initialContext.lookup(subContextName);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (NamingException) e.getCause();
      }
   }
   
   static private NamingEnumeration listBindings(final Context context, final String bindName)
   throws NamingException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return context.listBindings(bindName);
      }

      try
      {
         return (NamingEnumeration) AccessController.doPrivileged( new PrivilegedExceptionAction() 
         {
            public Object run() throws NamingException
            {
               return context.listBindings(bindName);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (NamingException) e.getCause();
      }
   }
   
   static private void rebind(final Context context, final String name, final Object object)
   throws NamingException
   {
      if (SecurityUtility.skipAccessControl())
      {
         context.rebind(name, object);
         return;
      }

      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction() 
         {
            public Object run() throws NamingException
            {
               context.rebind(name, object);
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (NamingException) e.getCause();
      }
   }
   
   static private void unbind(final Context context, final String name)
   throws NamingException
   {
      if (SecurityUtility.skipAccessControl())
      {
         context.unbind(name);
         return;
      }

      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction() 
         {
            public Object run() throws NamingException
            {
               context.unbind(name);
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (NamingException) e.getCause();
      }
   }
   
   static private InitialContext createContext(final Properties env) throws NamingException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return new InitialContext(env);
      }
      
      try
      {
         return (InitialContext) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return new InitialContext(env);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RuntimeException) e.getCause();
      }
   }
}
