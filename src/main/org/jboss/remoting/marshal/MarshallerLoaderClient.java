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
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.marshal.serializable.SerializableMarshaller;
import org.jboss.remoting.marshal.serializable.SerializableUnMarshaller;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to load marshaller and unmarshallers from remote server.
 *
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class MarshallerLoaderClient implements MarshallerLoaderConstants
{
   protected final static Logger log = Logger.getLogger(MarshallerLoaderClient.class);

   /**
    * Will call on marshall loader server to load marshaller for given data type.
    *
    * @param loaderLocator   converted locator indicating which marhaller loader to call upon.
    * @param dataType        indicates which marshaller to get.
    * @param classByteLoader is the class loader that the new marshaller and related classes get loaed into.
    * @return
    */
   public static Marshaller getMarshaller(InvokerLocator loaderLocator, String dataType, ClassLoader classByteLoader)
   {

      Marshaller marshaller = null;
      try
      {
         SerializableMarshaller loaderMarshaller = new SerializableMarshaller();
         SerializableUnMarshaller loaderUnMarshaller = new SerializableUnMarshaller();
         loaderUnMarshaller.setClassLoader(classByteLoader);
         
         String serializationType = "java";
         Map parameters = loaderLocator.getParameters();
         if (parameters != null)
         {
            Object o = parameters.get(InvokerLocator.SERIALIZATIONTYPE);
            if (o == null)
            {
               o = parameters.get(InvokerLocator.SERIALIZATIONTYPE_CASED);;
            }
            if (o != null)
            {
               serializationType = (String) o;
            }
         }
         loaderMarshaller.setSerializationType(serializationType);
         loaderUnMarshaller.setSerializationType(serializationType);
         
         String marshallerMethodName = GET_MARSHALLER_METHOD;
         Map metadata = new HashMap();
         metadata.put(InvokerLocator.DATATYPE, dataType);
         Client loaderClient = new Client(loaderLocator);
         loaderClient.connect();
         loaderClient.setMarshaller(loaderMarshaller);
         loaderClient.setUnMarshaller(loaderUnMarshaller);

         Object obj = null;
         obj = loaderClient.invoke(marshallerMethodName, metadata);
         if(obj != null)
         {
            marshaller = (Marshaller) obj;
         }

      }
      catch(Throwable e)
      {
         log.error("Error creating remoting client to connect to marhsaller loader.", e);
      }
      if(marshaller == null)
      {
         log.error("Can not load marshall for loader locator " + loaderLocator + ".");
      }

      return marshaller;
   }

   /**
    * Will call on marshall loader server to load unmarshaller for given data type.
    *
    * @param loaderLocator   converted locator indicating which marhaller loader to call upon.
    * @param dataType        indicates which unmarshaller to get.
    * @param classByteLoader is the class loader that the new unmarshaller and related classes get loaed into.
    * @return
    */
   public static UnMarshaller getUnMarshaller(InvokerLocator loaderLocator, String dataType, ClassLoader classByteLoader)
   {

      UnMarshaller unmarshaller = null;

      try
      {
         SerializableMarshaller loaderMarshaller = new SerializableMarshaller();
         SerializableUnMarshaller loaderUnMarshaller = new SerializableUnMarshaller();
         loaderUnMarshaller.setClassLoader(classByteLoader);
         
         String serializationType = "java";
         Map parameters = loaderLocator.getParameters();
         if (parameters != null)
         {
            Object o = parameters.get(InvokerLocator.SERIALIZATIONTYPE);
            if (o == null)
            {
               o = parameters.get(InvokerLocator.SERIALIZATIONTYPE_CASED);;
            }
            if (o != null)
            {
               serializationType = (String) o;
            }
         }
         loaderMarshaller.setSerializationType(serializationType);
         loaderUnMarshaller.setSerializationType(serializationType);

         String marshallerMethodName = GET_UNMARSHALLER_METHOD;
         Map metadata = new HashMap();
         metadata.put(InvokerLocator.DATATYPE, dataType);
         Client loaderClient = new Client(loaderLocator);
         loaderClient.connect();
         loaderClient.setMarshaller(loaderMarshaller);
         loaderClient.setUnMarshaller(loaderUnMarshaller);

         Object obj = null;

         obj = loaderClient.invoke(marshallerMethodName, metadata);
         if(obj != null)
         {
            unmarshaller = (UnMarshaller) obj;
         }
      }
      catch(Throwable e)
      {
         log.error("Error creating remoting client to connecto to marhsaller loader.", e);
      }
      if(unmarshaller == null)
      {
         log.error("Can not load unmarshaller for loader locator " + loaderLocator + ".");
      }
      return unmarshaller;
   }
}
