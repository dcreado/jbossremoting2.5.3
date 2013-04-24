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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;

import org.jboss.logging.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transport.Connector;

/**
 * Creates the marshall loader server to process requests from remoting clients to load marshaller/unmarshallers.
 *
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class MarshallLoaderFactory
{
   protected final static Logger log = Logger.getLogger(MarshallLoaderFactory.class);

   /**
    * Create a remoting server connector for a marshaller loader based on the specified locator.
    * The locator must contain at least the port parameter for which the marshaller loader should
    * listen on.  Will return null if can not create the connector.
    *
    * @param locator
    * @param classLoaders
    * @param mbeanServer
    * @return
    */
   public static Connector createMarshallLoader(InvokerLocator locator, List classLoaders, MBeanServer mbeanServer)
   {
      Connector marshallerConnector = null;
      try
      {
         marshallerConnector = new MarshallLoaderConnector();
         marshallerConnector.setInvokerLocator(locator.getLocatorURI());
         marshallerConnector.start();

         MarshallerLoaderHandler loader = new MarshallerLoaderHandler(classLoaders);
         marshallerConnector.addInvocationHandler("loader", loader);
         
         // Set after Connector.addInvocationHandler(), which also sets MBeanServer.
         loader.setMBeanServer(mbeanServer);
      }
      catch(Exception e)
      {
         log.error("Can not create marshaller loader.", e);
         if(marshallerConnector != null)
         {
            try
            {
               marshallerConnector.stop();
               marshallerConnector.destroy();
            }
            catch(Exception e1)
            {
               log.error("Error cleaning up marshaller loader connector.", e1);
            }
         }
      }
      return marshallerConnector;
   }

   /**
    * Will take regular invoker locator with extra parameters indicating marshall loader configuration (such as port)
    * and converts to the marshall loader locator.  Note: the transport returned will always be socket, as this is
    * the only transport protocol supported for dynamic marshall loading.
    *
    * @param locator
    * @return
    */
   public static InvokerLocator convertLocator(InvokerLocator locator)
   {
      InvokerLocator loaderLocator = null;

      if(locator != null)
      {
         Map params = locator.getParameters();
         if(params != null)
         {
            String sPort = (String) params.get(InvokerLocator.LOADER_PORT);
            if(sPort != null)
            {
               try
               {
                  int port = Integer.parseInt(sPort);
                  // Force to be socket, as is only one supported
                  //String transport = locator.getProtocol();
                  String transport = "socket";
                  String host = locator.getHost();
                  String path = locator.getPath();
                  Map metadata = new HashMap();
                  metadata.putAll(locator.getParameters());
                  // need to remove a few marshall related parameters so will not conflict
                  metadata.remove(InvokerLocator.LOADER_PORT);
                  metadata.remove(InvokerLocator.MARSHALLER);
                  metadata.remove(InvokerLocator.UNMARSHALLER);
                  metadata.remove(InvokerLocator.DATATYPE);
                  metadata.remove(InvokerLocator.DATATYPE_CASED);
                  loaderLocator = new InvokerLocator(transport, host, port, path, metadata);
               }
               catch(NumberFormatException e)
               {
                  log.error("Got loader port (" + sPort + ") from locator uri, but was not an number.");
               }
            }
         }
      }
      return loaderLocator;
   }
}