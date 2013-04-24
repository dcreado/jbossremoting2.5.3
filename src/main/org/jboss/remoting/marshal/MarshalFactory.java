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

package org.jboss.remoting.marshal;

import org.jboss.logging.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.marshal.http.HTTPMarshaller;
import org.jboss.remoting.marshal.http.HTTPUnMarshaller;
import org.jboss.remoting.marshal.rmi.RMIMarshaller;
import org.jboss.remoting.marshal.rmi.RMIUnMarshaller;
import org.jboss.remoting.marshal.serializable.SerializableMarshaller;
import org.jboss.remoting.marshal.serializable.SerializableUnMarshaller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * This class will provide marshallers and unmarshallers for data based on
 * the data type want to marshal to.  The most common will be just to serialize
 * the data.  However, may have jaxrpc, IIOP, serializers.  Can also have marshallers
 * and unmarshallers based on class type.  For example, might be marshaller/unmarshaller
 * for the Transaction class.
 *
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class MarshalFactory
{
   private static Map marshallers = Collections.synchronizedMap(new HashMap());
   private static Map unmarshallers = Collections.synchronizedMap(new HashMap());
   private static Map classMarshallers = Collections.synchronizedMap(new HashMap());
   private static Map classUnmarshallers = Collections.synchronizedMap(new HashMap());

   protected final static Logger log = Logger.getLogger(MarshalFactory.class);

   private final static boolean isTrace = log.isTraceEnabled();
   private final static boolean isDebug = log.isDebugEnabled();

   // statically load core marshallers/unmarshallers
   static
   {
      try
      {
         marshallers.put(SerializableMarshaller.DATATYPE, new SerializableMarshaller());
         unmarshallers.put(SerializableUnMarshaller.DATATYPE, new SerializableUnMarshaller());
         marshallers.put(HTTPMarshaller.DATATYPE, new HTTPMarshaller());
         unmarshallers.put(HTTPUnMarshaller.DATATYPE, new HTTPUnMarshaller());
         marshallers.put(RMIMarshaller.DATATYPE, new RMIMarshaller());
         unmarshallers.put(RMIUnMarshaller.DATATYPE, new RMIUnMarshaller());
      }
      catch(Exception e)
      {
         log.error("Could not statically load default marshallers.", e);
      }
   }

   /**
    * Will add the marshaller and unmarshaller based on class type.  Each can then be retrieved using the
    * class type as key.
    *
    * @param classType
    * @param marshaller
    * @param unMarshaller
    */
   public static void addMarshaller(Class classType, Marshaller marshaller, UnMarshaller unMarshaller)
   {
      classMarshallers.put(classType, marshaller);
      classUnmarshallers.put(classType, unMarshaller);
   }

   /**
    * Adds the marshaller and unmarshaller based on data type.  Each can then be retrieved using the data type
    * as the key.
    *
    * @param dataType
    * @param marshaller
    * @param unMarshaller
    */
   public static void addMarshaller(String dataType, Marshaller marshaller, UnMarshaller unMarshaller)
   {
      marshallers.put(dataType, marshaller);
      unmarshallers.put(dataType, unMarshaller);
   }

   /**
    * Looks up marshaller by class type.  Will return null if not found.
    *
    * @param classType
    * @return
    */
   public static Marshaller getMarshaller(Class classType)
   {
      Marshaller marshaller = null;
      Object obj = classMarshallers.get(classType);
      if(obj != null && obj instanceof Marshaller)
      {
         marshaller = (Marshaller) obj;

         try
         {
            marshaller = marshaller.cloneMarshaller();
         }
         catch(CloneNotSupportedException e)
         {
            log.warn("Could not clone " + marshaller);
         }
      }
      else
      {
         if(isTrace)
         {
            log.trace("Could not find marshaller for class type '" + classType + "'.  Object in collection is " + obj);
         }
      }

      return marshaller;
   }

   /**
    * Looks up marshaller by class type.  Will return null if not found.
    *
    * @param classType
    * @return
    */
   public static Marshaller getMarshaller(Class classType, String serializationType)
   {
      Marshaller marshaller = getMarshaller(classType);
      if(marshaller instanceof SerialMarshaller)
      {
         ((SerialMarshaller) marshaller).setSerializationType(serializationType);
      }
      return marshaller;
   }

   /**
    * Returns unmarshaller by class type.  Will return null if not found.
    *
    * @param classType
    * @return
    */
   public static UnMarshaller getUnMarshaller(Class classType)
   {
      UnMarshaller unmarshaller = null;
      Object obj = classUnmarshallers.get(classType);
      if(obj != null && obj instanceof UnMarshaller)
      {
         unmarshaller = (UnMarshaller) obj;

         try
         {
            unmarshaller = unmarshaller.cloneUnMarshaller();
         }
         catch(CloneNotSupportedException e)
         {
            log.warn("Could not clone " + unmarshaller);
         }

      }
      else
      {
         if(isTrace)
         {
            log.trace("Could not find unmarshaller for class type '" + classType + "'.  Object in collection is " + obj);
         }
      }
      return unmarshaller;
   }

   public static UnMarshaller getUnMarshaller(Class classType, String serializationType)
   {
      UnMarshaller unmarshaller = getUnMarshaller(classType);
      if(unmarshaller instanceof SerializableUnMarshaller)
      {
         ((SerializableUnMarshaller) unmarshaller).setSerializationType(serializationType);
      }

      return unmarshaller;
   }

   /**
    * Gets marshaller based on data type (i.e. serializable) and based on the marshallers registered with the factory.
    *
    * @param dataType
    * @return The marshaller or null if none for for the specified type
    */
   public static Marshaller getMarshaller(String dataType)
   {
      Marshaller marshaller = null;
      Object obj = marshallers.get(dataType);
      if(obj != null && obj instanceof Marshaller)
      {
         marshaller = (Marshaller) obj;

         try
         {
            marshaller = marshaller.cloneMarshaller();
         }
         catch(CloneNotSupportedException e)
         {
            log.warn("Could not clone " + marshaller);
         }

      }
      else
      {
         if(isTrace)
         {
            log.trace("Could not find marshaller for data type '" + dataType + "'.  Object in collection is " + obj);
         }
      }

      return marshaller;
   }


   public static Marshaller getMarshaller(String dataType, String serializationType)
   {
      Marshaller marshaller = getMarshaller(dataType);
      if(marshaller instanceof SerializableMarshaller)
      {
         ((SerializableMarshaller) marshaller).setSerializationType(serializationType);
      }
      return marshaller;
   }


   public static UnMarshaller getUnMarshaller(String dataType, String serializationType)
   {
      UnMarshaller unmarshaller = getUnMarshaller(dataType);
      if(unmarshaller instanceof SerializableUnMarshaller)
      {
         ((SerializableUnMarshaller) unmarshaller).setSerializationType(serializationType);
      }

      return unmarshaller;
   }


   /**
    * Gets the marshaller based on data type (i.e. serialziable) and based on the unmarshallers registered with the factory.
    *
    * @param dataType
    * @return The unmarshaller or null if none for the specified type
    */
   public static UnMarshaller getUnMarshaller(String dataType)
   {
      UnMarshaller unmarshaller = null;
      Object obj = unmarshallers.get(dataType);
      if(obj != null && obj instanceof UnMarshaller)
      {
         unmarshaller = (UnMarshaller) obj;

         try
         {
            unmarshaller = unmarshaller.cloneUnMarshaller();
         }
         catch(CloneNotSupportedException e)
         {
            log.warn("Could not clone " + unmarshaller);
         }
      }
      else
      {
         if(isTrace)
         {
            log.trace("Could not find unmarshaller for data type '" + dataType + "'.  Object in collection is " + obj);
         }
      }

      return unmarshaller;
   }

   /**
    * Will try to look up marshaller by first looking for data type parameter within locator and then using that
    * to look up marhsaller locally.  If can not find it, will then look to see if can find the 'marshaller' parameter
    * within the locator parameters.  If found, will try to load the marshaller by the class name specified as the parameter
    * value.  If still can not find the class within the local VM, will look to see if there is a parameter for
    * the server's marshaller loader port.  If this exists, will then try calling on the remote server to load the
    * marshaller (and its related classes) within the local VM.  If still can not be found, will return null.
    *
    * @param locator
    * @param classLoader
    * @return
    */
   public static Marshaller getMarshaller(InvokerLocator locator, ClassLoader classLoader)
   {
      return getMarshaller(locator, classLoader, null);
   }

   /**
    * Will try to look up marshaller by first looking for data type parameter within locator and config and then using that
    * to look up marhsaller locally.  If can not find it, will then look to see if can find the 'marshaller' parameter
    * within the locator parameters or config parameters.  If found, will try to load the marshaller by the class name specified
    * as the parameter value.  If still can not find the class within the local VM, will look to see if there is a parameter for
    * the server's marshaller loader port.  If this exists, will then try calling on the remote server to load the
    * marshaller (and its related classes) within the local VM.  If still can not be found, will return null.
    *
    * @param locator
    * @param classLoader
    * @param config
    * @return
    */
   public static Marshaller getMarshaller(InvokerLocator locator, ClassLoader classLoader, Map config)
   {
      String serializationType = locator.findSerializationType();
      Marshaller marshaller = null;
      if(locator != null || config != null)
      {
         Map params = new HashMap();
         if (locator.getParameters() != null)
         {
            params.putAll(locator.getParameters());
         }
         if (config != null)
         {
            params.putAll(config);
         }
         if(params != null)
         {
            // start with data type as is prefered method of getting marshaller/unmarshaller
            String dataType = (String) params.get(InvokerLocator.DATATYPE);
            if(dataType == null)
            {
               dataType = (String) params.get(InvokerLocator.DATATYPE_CASED);
            }
            if(dataType != null)
            {
               marshaller = getMarshaller(dataType);
            }
            if(marshaller == null)
            {
               if(isTrace)
               {
                  log.trace("Could not look up marshaller by data type ('" + dataType + "').  Will try to load dynamically.");
               }

               // will now look for explicit param for marshaller class
               String marshallerFQN = (String) params.get(InvokerLocator.MARSHALLER);
               marshaller = loadMarshaller(marshallerFQN);
               if(marshaller != null)
               {
                  if(isTrace)
                  {
                     log.trace("Found marshaller by loading locally.");
                  }
                  // try to load unmarshaller so that can add to list
                  String unmarshallerFQN = (String) params.get(InvokerLocator.UNMARSHALLER);
                  UnMarshaller unmarshaller = loadUnMarshaller(unmarshallerFQN);
                  if(unmarshaller != null)
                  {
                     addMarshaller(dataType, marshaller, unmarshaller);
                  }
               }
            }
            if(marshaller == null && isTrace)
            {
               log.trace("Tried to find marshaller from locator by both data type and class name but was unsuccessful.  " +
                         "Will try to load it from remote server.");
            }
            // if still have not found marshaller, check to see if can load remotely
            if(marshaller == null && dataType != null)
            {
               InvokerLocator loaderLocator = MarshallLoaderFactory.convertLocator(locator);
               if(loaderLocator != null)
               {
                  marshaller = MarshallerLoaderClient.getMarshaller(loaderLocator, dataType, classLoader);
                  UnMarshaller unmarshaller = MarshallerLoaderClient.getUnMarshaller(loaderLocator, dataType, classLoader);
                  if(unmarshaller != null)
                  {
                     unmarshaller.setClassLoader(classLoader);
                  }
                  if(isDebug)
                  {
                     log.debug("Remotely loaded marshaller: " + marshaller);
                     log.debug("Remotely loaded unmarshaller: " + unmarshaller);
                  }
                  if(marshaller != null && unmarshaller != null)
                  {
                     addMarshaller(dataType, marshaller, unmarshaller);
                  }
               }
            }
         }
      }

      if(marshaller != null)
      {
         try
         {
            marshaller = marshaller.cloneMarshaller();
         }
         catch(CloneNotSupportedException e)
         {
            log.warn("Could not clone " + marshaller);
         }
      }

      if(marshaller instanceof SerialMarshaller)
      {
         ((SerialMarshaller) marshaller).setSerializationType(serializationType);
      }
      return marshaller;
   }

   private static Marshaller loadMarshaller(String marshallerFQN)
   {
      Marshaller marshaller = null;
      if(marshallerFQN != null)
      {
         try
         {
            Class marshallerClass = Class.forName(marshallerFQN);
            marshaller = (Marshaller) marshallerClass.newInstance();
         }
         catch(Exception e)
         {
            log.warn("Found marshaller fully qualified class name within locator parameters, but was unable " +
                     "to load class: " + marshallerFQN);
         }
      }
      return marshaller;
   }

   /**
    * Will try to look up unmarshaller by first looking for data type parameter within locator and then using that
    * to look up unmarshaller locally.  If can not find it, will then look to see if can find the 'unmarshaller' parameter
    * within the locator parameters.  If found, will try to load the unmarshaller by the class name specified as the parameter
    * value.  If still can not find the class within the local VM, will look to see if there is a parameter for
    * the server's marshaller loader port.  If this exists, will then try calling on the remote server to load the
    * unmarshaller (and its related classes) within the local VM.  If still can not be found, will return null.
    *
    * @param locator
    * @param classLoader
    * @return
    */
   public static UnMarshaller getUnMarshaller(InvokerLocator locator, ClassLoader classLoader)
   {
      return getUnMarshaller(locator, classLoader, null);
   }

   /**
    * Will try to look up unmarshaller by first looking for data type parameter within locator and config map and then using that
    * to look up unmarshaller locally.  If can not find it, will then look to see if can find the 'unmarshaller' parameter
    * within the locator parameters or config map.  If found, will try to load the unmarshaller by the class name specified as the
    * parameter value.  If still can not find the class within the local VM, will look to see if there is a parameter for
    * the server's marshaller loader port.  If this exists, will then try calling on the remote server to load the
    * unmarshaller (and its related classes) within the local VM.  If still can not be found, will return null.
    *
    * @param locator
    * @param classLoader
    * @param config
    * @return
    */
   public static UnMarshaller getUnMarshaller(InvokerLocator locator, ClassLoader classLoader, Map config)
   {
      String serializationType = locator.findSerializationType();
      UnMarshaller unmarshaller = null;
      if(locator != null || config != null)
      {
         Map params = new HashMap();
         if (locator.getParameters() != null)
         {
            params.putAll(locator.getParameters());
         }
         if (config != null)
         {
            params.putAll(config);
         }
         if(params != null)
         {
            // start with data type as is prefered method of getting marshaller/unmarshaller
            String dataType = (String) params.get(InvokerLocator.DATATYPE);
            if(dataType == null)
            {
               dataType = (String) params.get(InvokerLocator.DATATYPE_CASED);
            }
            if(dataType != null)
            {
               unmarshaller = getUnMarshaller(dataType);
            }
            if(unmarshaller == null)
            {
               if(isTrace)
               {
                  log.trace("Could not find unmarshaller by data type ('" + dataType + "').  Will try to load dynamically.");
               }

               // will now look for explicit param for marshaller class
               String unmarshallerFQN = (String) params.get(InvokerLocator.UNMARSHALLER);
               unmarshaller = loadUnMarshaller(unmarshallerFQN);
               if(unmarshaller != null)
               {
                  String marshallerFQN = (String) params.get(InvokerLocator.MARSHALLER);
                  Marshaller marshaller = loadMarshaller(marshallerFQN);
                  if(marshaller != null)
                  {
                     addMarshaller(dataType, marshaller, unmarshaller);
                  }
               }
            }
            if(isTrace && unmarshaller == null)
            {
               log.trace("Tried to find unmarshaller from locator by both data type and class name but was unsuccessful.");
            }
            // if still have not found unmarshaller, check to see if can load remotely
            if(unmarshaller == null && dataType != null)
            {
               InvokerLocator loaderLocator = MarshallLoaderFactory.convertLocator(locator);
               unmarshaller = MarshallerLoaderClient.getUnMarshaller(loaderLocator, dataType, classLoader);
               if(unmarshaller != null)
               {
                  unmarshaller.setClassLoader(classLoader);
               }
               Marshaller marshaller = MarshallerLoaderClient.getMarshaller(loaderLocator, dataType, classLoader);
               if(isTrace)
               {
                  log.trace("Remotely loaded marshaller: " + marshaller);
                  log.trace("Remotely loaded unmarshaller: " + unmarshaller);
               }
               if(marshaller != null && unmarshaller != null)
               {
                  addMarshaller(dataType, marshaller, unmarshaller);
               }
            }
         }
      }

      if(unmarshaller != null)
      {
         try
         {
            unmarshaller = unmarshaller.cloneUnMarshaller();
         }
         catch(CloneNotSupportedException e)
         {
            log.warn("Could not clone " + unmarshaller);
         }
      }
      if(unmarshaller instanceof SerializableUnMarshaller)
      {
         ((SerializableUnMarshaller) unmarshaller).setSerializationType(serializationType);
      }
      return unmarshaller;
   }

   private static UnMarshaller loadUnMarshaller(String unmarshallerFQN)
   {
      UnMarshaller unmarshaller = null;
      if(unmarshallerFQN != null)
      {
         try
         {
            Class unmarshallerClass = Class.forName(unmarshallerFQN);
            unmarshaller = (UnMarshaller) unmarshallerClass.newInstance();
         }
         catch(Exception e)
         {
            log.error("Found unmarshaller fully qualified class name within locator parameters, but was unable " +
                      "to load class: " + unmarshallerFQN, e);
         }
      }
      return unmarshaller;
   }
}