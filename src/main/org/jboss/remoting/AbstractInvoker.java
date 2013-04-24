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
import org.jboss.remoting.loading.ClassByteClassLoader;
import org.jboss.remoting.marshal.MarshallLoaderFactory;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.serialization.SerializationStreamFactory;
import org.jboss.remoting.socketfactory.CreationListenerSocketFactory;
import org.jboss.remoting.socketfactory.SocketCreationListener;
import org.jboss.remoting.socketfactory.SocketFactoryWrapper;
import org.jboss.remoting.serialization.ClassLoaderUtility;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.util.id.GUID;

import javax.net.SocketFactory;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

/**
 * AbstractInvoker is an abstract handler part that contains common methods between both
 * client and server.
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @version $Revision: 5271 $
 */
public abstract class AbstractInvoker implements Invoker
{
   protected final static Logger log = Logger.getLogger(AbstractInvoker.class);
   protected ClassByteClassLoader classbyteloader;
   protected InvokerLocator locator;
   protected Map localServerLocators = new HashMap();
   protected String serializationType;
   protected Map configuration = new HashMap();
   protected SocketFactory socketFactory;
   protected int version;
   protected boolean passConfigMapToMarshalFactory;

   // Indicates if the serverSocketFactory was generated internally.
   protected boolean socketFactoryCreatedFromSSLParameters;

   public AbstractInvoker(InvokerLocator locator)
   {
      this(locator, null);
   }

   public AbstractInvoker(InvokerLocator locator, Map configuration)
   {
	  try
	  {
	      this.classbyteloader = (ClassByteClassLoader)AccessController.doPrivileged( new PrivilegedExceptionAction()
	      {
	         public Object run() throws Exception
	         {
	        	 return new ClassByteClassLoader(AbstractInvoker.class.getClassLoader());
	         }
	      });
	  }
	  catch (PrivilegedActionException e)
	  {
		  log.debug(e.toString(), e);
		  throw new RuntimeException("Can't create a ClassLoader", e);
	  }
      this.locator = locator;

      if (checkConfigOverridesLocator(locator, configuration))
      {
         if (locator.getParameters() != null)
            this.configuration.putAll(locator.getParameters());
         
         if (configuration != null)
            this.configuration.putAll(configuration);
      }
      else
      {
         if (configuration != null)
            this.configuration.putAll(configuration);

         if (locator.getParameters() != null)
            this.configuration.putAll(locator.getParameters());
      }
      try
      {
         InvokerLocator loaderLocator = MarshallLoaderFactory.convertLocator(locator);
         if(loaderLocator != null)
         {
            classbyteloader.setClientInvoker(new Client(loaderLocator));
         }
      }
      catch(Exception e)
      {
         log.error("Could not create remote class loading for invoker.", e);
      }

      if(locator == null || locator.getParameters() == null)
      {
         this.setSerializationType(SerializationStreamFactory.JAVA);
      }
      else
      {
         this.setSerializationType(locator.findSerializationType());
      }

      setVersion(Version.getDefaultVersion());
      Object o = this.configuration.get(Remoting.REMOTING_VERSION);
      if (o instanceof String)
      {
         try
         {
            int v = Integer.valueOf((String)o).intValue();
            if (Version.isValidVersion(v))
            {
               log.debug(this + " setting version to " + v);
               setVersion(v);
            }
            else
            {
               log.debug(this + " invalid version: " + v + ". Using " + getVersion());
            }
         }
         catch (Exception e)
         {
            log.warn(this + " could not convert " + Remoting.REMOTING_VERSION +
                     " value of " + o + " to an int value. Using " + getVersion());
         }
      }
      else if (o != null)
      {
         log.warn(this + " value of " + Remoting.REMOTING_VERSION +
                  " must be a String: " + o + ". Using " + getVersion());  
      }
      
      o = this.configuration.get(Remoting.PASS_CONFIG_MAP_TO_MARSHAL_FACTORY);
      if (o instanceof String)
      {
         this.passConfigMapToMarshalFactory = Boolean.valueOf((String) o).booleanValue();
      }
      else if (o != null)
      {
         log.warn("value of " + Remoting.PASS_CONFIG_MAP_TO_MARSHAL_FACTORY + " must be a String: " + o); 
      }
   }

   /**
    * return the locator this Invoker represents
    */
   public InvokerLocator getLocator()
   {
      return locator;
   }

   /**
    * Generates a listener id for the callbackhandler and callback server locator combination
    * @param sessionId
    * @param locator
    */
   public String addClientLocator(String sessionId, InvokerCallbackHandler callbackhandler, InvokerLocator locator)
   {
      String listenerId = null;
      synchronized(localServerLocators)
      {
         Collection holders = localServerLocators.values();
         Iterator itr = holders.iterator();
         while(itr.hasNext())
         {
            CallbackHandlerHolder holder = (CallbackHandlerHolder)itr.next();
            InvokerCallbackHandler holderhandler = holder.getHandler();
            boolean handlersEqual = holderhandler.equals(callbackhandler);
            InvokerLocator handlerLocator = holder.getLocator();
            boolean locatorsEqual = handlerLocator.equals(locator);
            if(handlersEqual && locatorsEqual)
            {
               // the entry already exists for this pair, so return null
               return null;
            }
         }

         // if got this far, the entry does not exist, so need to add it and create a listener id
         CallbackHandlerHolder holder = new CallbackHandlerHolder(callbackhandler, locator);
         listenerId = new GUID().toString();
         String key = listenerId;
         if (sessionId != null)
            key = sessionId + "+" + listenerId;
         localServerLocators.put(key, holder);
      }

      return listenerId;
   }

   /**
    * Gets the client locator.  This locator will be used by the server side
    * to make callbacks to the handler for this locator.
    */
   public InvokerLocator getClientLocator(String listenerId)
   {
      InvokerLocator locator = null;
      if(listenerId != null)
      {
         CallbackHandlerHolder holder = (CallbackHandlerHolder) localServerLocators.get(listenerId);
         if(holder != null)
         {
            locator = holder.getLocator();
         }
      }
      return locator;
   }

   public List getClientLocators(String sessionId, InvokerCallbackHandler handler)
   {
      List holderList = new ArrayList();
      if(handler != null)
      {
         synchronized(localServerLocators)
         {
            Set entries = localServerLocators.entrySet();
            Iterator itr = entries.iterator();
            while(itr.hasNext())
            {
               Map.Entry entry = (Map.Entry) itr.next();
               String listenerId = (String) entry.getKey();
               int index = listenerId.indexOf('+');
               String prefix = listenerId.substring(0, index);
               if (!sessionId.equals(prefix))
                  continue;
               if (index >= 0)
                  listenerId = listenerId.substring(index + 1);
               CallbackHandlerHolder holder = (CallbackHandlerHolder) entry.getValue();
               InvokerCallbackHandler holderHandler = holder.getHandler();
               if(holderHandler.equals(handler))
               {
                  CallbackLocatorHolder locatorHolder = new CallbackLocatorHolder(listenerId, holder.getLocator());
                  holderList.add(locatorHolder);
               }
            }
            // now remove holders
            if(holderList.size() > 0)
            {
               for(int x = 0; x < holderList.size(); x++)
               {
                  String listenerId = ((CallbackLocatorHolder)holderList.get(x)).getListenerId();
                  String key = sessionId + "+" + listenerId;
                  localServerLocators.remove(key);
               }
            }
         }
      }
         return holderList;
   }

   /**
    * set the classloader to use internally
    *
    * @param classloader
    */
   public synchronized void setClassLoader(final ClassLoader classloader)
   {
	  try
	  {
	      this.classbyteloader = (ClassByteClassLoader)AccessController.doPrivileged( new PrivilegedExceptionAction()
	      {
	         public Object run() throws Exception
	         {
	        	 return new ClassByteClassLoader(classloader);
	         }
	      });
	  }
	  catch (PrivilegedActionException e)
	  {
		  log.debug(e.toString(), e);
		  throw new RuntimeException("Can't create a ClassLoader", e);
	  }
   }

   public synchronized ClassLoader getClassLoader()
   {
      return classbyteloader;
   }

   public String getSerializationType()
   {
      return serializationType;
   }

   public void setSerializationType(String serializationType)
   {
      this.serializationType = serializationType;
   }

   public SocketFactory getSocketFactory()
   {
      return socketFactory;
   }

   public void setSocketFactory(SocketFactory socketFactory)
   {
      this.socketFactory = socketFactory;
   }

   public boolean isSocketFactoryCreatedFromSSLParameters()
   {
      return socketFactoryCreatedFromSSLParameters;
   }

   public int getVersion()
   {
      return version;
   }

   public void setVersion(int version)
   {
      this.version = version;
   }

   /**
    * If any configuration parameters relate to the construction of a SSLSocketBuilder, create one.
    */
   protected SocketFactory createSocketFactory(Map configuration)
   {
      if (configuration == null)
         return null;

      if (socketFactory != null)
         return socketFactory;

      SocketFactory factory = null;

      Object obj = configuration.get(Remoting.CUSTOM_SOCKET_FACTORY);
      if (obj != null)
      {
         if (obj instanceof SocketFactory)
         {
            factory = (SocketFactory) obj;
         }
         else
         {
            throw new RuntimeException("Can not set custom socket factory (" + obj + ") as is not of type javax.net.SocketFactory");
         }
      }

      if(factory == null)
      {
         String socketFactoryString = (String)configuration.get(Remoting.SOCKET_FACTORY_NAME);
         if (socketFactoryString == null)
         {
            socketFactoryString = (String)configuration.get(Remoting.SOCKET_FACTORY_CLASS_NAME);            
         }
         
         if(socketFactoryString != null && socketFactoryString.length() > 0)
         {
            try
            {
               Class cl = ClassLoaderUtility.loadClass(socketFactoryString,  getClass());

               Constructor socketFactoryConstructor = null;
               socketFactoryConstructor = cl.getConstructor(new Class[]{});
               factory = (SocketFactory)socketFactoryConstructor.newInstance(new Object[] {});
               log.trace("SocketFactory (" + socketFactoryString + ") loaded");
            }
            catch(Exception e)
            {
               log.debug("Could not create socket factory by classname (" + socketFactoryString + ").  Error message: " + e.getMessage());
            }
         }
      }

      if (factory == null && needsCustomSSLConfiguration(configuration))
      {
         try
         {
            SSLSocketBuilder socketBuilder = new SSLSocketBuilder(configuration);
            socketBuilder.setUseSSLSocketFactory( false );
            factory = socketBuilder.createSSLSocketFactory();
            socketFactoryCreatedFromSSLParameters = true;
         }
         catch (IOException e)
         {
            throw new RuntimeException("Unable to create customized SSL socket factory", e);
         }
      }

      return wrapSocketFactory(factory, configuration);
   }
   
   protected Map getConfiguration()
   {
      return configuration;
   }
   
   protected boolean checkConfigOverridesLocator(InvokerLocator locator, Map config)
   {
      boolean result = false;
      if (config != null)
      {
         Object o = config.get(Remoting.CONFIG_OVERRIDES_LOCATOR);
         if (o != null)
         {
            if (o instanceof String)
            {
               result = Boolean.valueOf((String) o).booleanValue();
            }
            else
            {
               log.warn("value of " + Remoting.CONFIG_OVERRIDES_LOCATOR + " in configuration Map should be a String instead of: " + o);
            }
         }
      }
      Map map = locator.parameters;
      if (map != null)
      {
         Object o = map.get(Remoting.CONFIG_OVERRIDES_LOCATOR);
         if (o != null)
         {
            if (o instanceof String)
            {
               result = Boolean.valueOf((String) o).booleanValue();
            }
            else
            {
               log.warn("value of " + Remoting.CONFIG_OVERRIDES_LOCATOR + " in " + locator + " should be a String");
            }
         }
      }
      
      return result;
   }

   static public SocketFactory wrapSocketFactory(SocketFactory socketFactory, Map config)
   {
      if (config == null)
         return socketFactory;
      
      Object o = config.get(Remoting.SOCKET_CREATION_CLIENT_LISTENER);
      
      if (o == null)
         return socketFactory;
      
      if (o instanceof SocketCreationListener)
      {
         SocketCreationListener listener = (SocketCreationListener) o;
         return new CreationListenerSocketFactory(socketFactory, listener);
      }
      else if (o instanceof String)
      {
         try
         {
            Class c = ClassLoaderUtility.loadClass((String) o, AbstractInvoker.class);
            SocketCreationListener listener = (SocketCreationListener) c.newInstance();
            return new CreationListenerSocketFactory(socketFactory, listener);
         }
         catch (Exception e)
         {
            log.warn("unable to instantiate class: " + o, e);
            return socketFactory;
         }
      }
      else
      {
         log.warn("unrecognized type for socket creation client listener: " + o);
         return socketFactory;
      }
   }
   
   
   static public boolean isCompleteSocketFactory(SocketFactory sf)
   {
      if (sf != null)
      {
         if (!(sf instanceof SocketFactoryWrapper) ||
             ((SocketFactoryWrapper)sf).getSocketFactory() != null)
            return true;
      }
      return false;
   }


   public static boolean needsCustomSSLConfiguration(Map configuration)
   {
      if (configuration.get(SSLSocketBuilder.REMOTING_KEY_ALIAS) != null ||
            configuration.get(SSLSocketBuilder.REMOTING_CLIENT_AUTH_MODE) != null ||
            configuration.get(SSLSocketBuilder.REMOTING_SERVER_AUTH_MODE) != null ||
            configuration.get(SSLSocketBuilder.REMOTING_SSL_PROTOCOL) != null ||
            configuration.get(SSLSocketBuilder.REMOTING_SSL_PROVIDER_NAME) != null ||
            configuration.get(SSLSocketBuilder.REMOTING_SERVER_SOCKET_USE_CLIENT_MODE) != null ||
            configuration.get(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE) != null ||
            configuration.get(SSLSocketBuilder.REMOTING_KEY_PASSWORD) != null ||
            configuration.get(SSLSocketBuilder.REMOTING_KEY_STORE_ALGORITHM) != null ||
            configuration.get(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH) != null ||
            configuration.get(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD) != null ||
            configuration.get(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE) != null ||
            configuration.get(SSLSocketBuilder.REMOTING_TRUST_STORE_ALGORITHM) != null ||
            configuration.get(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH) != null ||
            configuration.get(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD) != null ||
            configuration.get(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE) != null
      )
         return true;
      else
         return false;
   }

   private class CallbackHandlerHolder
   {
      private InvokerCallbackHandler handler;
      private InvokerLocator locator;

      private CallbackHandlerHolder(InvokerCallbackHandler handler, InvokerLocator locator)
      {
         this.handler = handler;
         this.locator = locator;
      }

      public InvokerCallbackHandler getHandler()
      {
         return handler;
      }

      public InvokerLocator getLocator()
      {
         return locator;
      }
   }

   public class CallbackLocatorHolder
   {
      private InvokerLocator locator;
      private String listenerId;

      public CallbackLocatorHolder(String listenerId, InvokerLocator locator)
      {
         this.listenerId = listenerId;
         this.locator = locator;
      }

      public String getListenerId()
      {
         return listenerId;
      }

      public InvokerLocator getLocator()
      {
         return locator;
      }
   }
}
