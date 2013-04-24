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
package org.jboss.test.remoting.transport.servlet.marshal;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transport.http.HTTPMetadataConstants;
import org.jboss.test.remoting.transport.http.marshal.HttpContentTypeTestCase;


/**
 * Unit tests for JBREM-1145.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright August 17, 2009
 * </p>
 */
public class ContentTypeTestClient extends HttpContentTypeTestCase
{
   private static Logger log = Logger.getLogger(ContentTypeTestClient.class);
   
   
   protected void validateOrdinaryInvocation(Client client) throws Throwable
   {
      // Local tests
      log.info("TestMarshaller.marshallers.size(): " + TestMarshaller.marshallers.size());
      log.info("TestMarshaller.unmarshallers.size(): " + TestUnMarshaller.unmarshallers.size());
      assertEquals(2, TestMarshaller.marshallers.size());
      assertEquals(HTTPMetadataConstants.REMOTING_CONTENT_TYPE_NON_STRING, ((TestMarshaller)TestMarshaller.marshallers.get(1)).type);
      assertEquals(2, TestUnMarshaller.unmarshallers.size());
      assertEquals(HTTPMetadataConstants.REMOTING_CONTENT_TYPE_NON_STRING, ((TestUnMarshaller)TestUnMarshaller.unmarshallers.get(1)).type);

      // Remote tests
      int serverMarshallerCount = ((Integer) client.invoke(TestInvocationHandler.GET_NUMBER_OF_MARSHALLERS)).intValue();
      log.info("server side marshallers: " + serverMarshallerCount);
      int serverUnmarshallerCount = ((Integer) client.invoke(TestInvocationHandler.GET_NUMBER_OF_UNMARSHALLERS)).intValue();
      log.info("server side unmarshallers: " + serverUnmarshallerCount);
      Map metadata = new HashMap();
      metadata.put(TestInvocationHandler.N, Integer.toString(serverMarshallerCount - 1));
      String type = (String) client.invoke(TestInvocationHandler.GET_NTH_MARSHALLER_TYPE, metadata);
      assertEquals(HTTPMetadataConstants.REMOTING_CONTENT_TYPE_NON_STRING, type);
      metadata.put(TestInvocationHandler.N, Integer.toString(serverUnmarshallerCount - 1));
      type = (String) client.invoke(TestInvocationHandler.GET_NTH_UNMARSHALLER_TYPE, metadata);
      assertEquals(HTTPMetadataConstants.REMOTING_CONTENT_TYPE_NON_STRING, type);
      client.invoke(TestInvocationHandler.RESET);
   }
   
   
   protected void validateRawStringMessage(Client client) throws Throwable
   {
      // Local tests
      log.info("TestMarshaller.marshallers.size(): " + TestMarshaller.marshallers.size());
      log.info("TestMarshaller.unmarshallers.size(): " + TestUnMarshaller.unmarshallers.size());
      assertEquals(2, TestMarshaller.marshallers.size());
      assertEquals(HTTPMetadataConstants.REMOTING_CONTENT_TYPE_STRING, ((TestMarshaller)TestMarshaller.marshallers.get(1)).type);
      assertEquals(2, TestUnMarshaller.unmarshallers.size());
      assertEquals(HTTPMetadataConstants.REMOTING_CONTENT_TYPE_STRING, ((TestUnMarshaller)TestUnMarshaller.unmarshallers.get(1)).type);

      // Remote tests
      int serverMarshallerCount = ((Integer) client.invoke(TestInvocationHandler.GET_NUMBER_OF_MARSHALLERS)).intValue();
      log.info("server side marshallers: " + serverMarshallerCount);
      int serverUnmarshallerCount = ((Integer) client.invoke(TestInvocationHandler.GET_NUMBER_OF_UNMARSHALLERS)).intValue();
      log.info("server side unmarshallers: " + serverUnmarshallerCount);
      assertEquals(4, serverMarshallerCount);
      assertEquals(6, serverUnmarshallerCount);
      Map metadata = new HashMap();
      metadata.put(TestInvocationHandler.N, "3");
      String type = (String) client.invoke(TestInvocationHandler.GET_NTH_MARSHALLER_TYPE, metadata);
      assertEquals(HTTPMetadataConstants.REMOTING_CONTENT_TYPE_STRING, type);
      metadata.put(TestInvocationHandler.N, "1");
      type = (String) client.invoke(TestInvocationHandler.GET_NTH_UNMARSHALLER_TYPE, metadata);
      assertEquals(HTTPMetadataConstants.REMOTING_CONTENT_TYPE_STRING, type);
      client.invoke(TestInvocationHandler.RESET);
   }
   
   protected String getTransport()
   {
      return "servlet";
   }
   
   protected void setupServer(boolean addUseRemotingContentType, boolean useRemotingContentType) throws Exception
   {
      String path = null;
      String useRemotingContentTypeAttribute = null;
      if (addUseRemotingContentType)
      {
         if (useRemotingContentType)
         {
            path = "servlet-invoker/ServerInvokerServlet/true";
            useRemotingContentTypeAttribute = "&useRemotingContentType=true";         
         }
         else
         {
            path = "servlet-invoker/ServerInvokerServlet/false";
            useRemotingContentTypeAttribute = "&useRemotingContentType=false";
         }
      }
      else
      {
         path = "servlet-invoker/ServerInvokerServlet/default";
         useRemotingContentTypeAttribute = "";
      }
      
      String locatorURI = "servlet://localhost:8080/" + path + "/?datatype=test&marshaller=org.jboss.test.remoting.marshall.TestMarshaller&unmarshaller=org.jboss.test.remoting.marshall.TestUnmarshaller" + useRemotingContentTypeAttribute;
      serverLocator = new InvokerLocator(locatorURI);
   }
}