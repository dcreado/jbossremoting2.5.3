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

package org.jboss.remoting.transport.servlet;

import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvocationResponse;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.Remoting;
import org.jboss.remoting.marshal.MarshalFactory;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.marshal.VersionedMarshaller;
import org.jboss.remoting.marshal.VersionedUnMarshaller;
import org.jboss.remoting.marshal.http.HTTPMarshaller;
import org.jboss.remoting.marshal.http.HTTPUnMarshaller;
import org.jboss.remoting.transport.http.HTTPMetadataConstants;
import org.jboss.remoting.transport.web.WebServerInvoker;
import org.jboss.remoting.transport.web.WebUtil;
import org.jboss.remoting.util.SecurityUtility;
import org.jboss.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;

/**
 * The servlet based server invoker that receives the original http request
 * from the ServerInvokerServlet.
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class ServletServerInvoker extends WebServerInvoker implements ServletServerInvokerMBean
{
   public static final String UNWRAP_SINGLETON_ARRAYS = "unwrapSingletonArrays";
   
   private static final Logger log = Logger.getLogger(ServletServerInvoker.class);
   
   private boolean unwrapSingletonArrays;
   
   private boolean useRemotingContentType;

   public ServletServerInvoker(InvokerLocator locator)
   {
      super(locator);
      init();
   }

   public ServletServerInvoker(InvokerLocator locator, Map configuration)
   {
      super(locator, configuration);
      init();
   }

   protected String getDefaultDataType()
   {
      return HTTPMarshaller.DATATYPE;
   }
   
   protected void init()
   {
      Object val = configuration.get(UNWRAP_SINGLETON_ARRAYS);
      if (val != null)
      {
         try
         {
            unwrapSingletonArrays = Boolean.valueOf((String)val).booleanValue();
            log.debug(this + " setting unwrapSingletonArrays to " + unwrapSingletonArrays);
         }
         catch (Exception e)
         {
            log.warn(this + " could not convert " + 
                     UNWRAP_SINGLETON_ARRAYS + " value of " +
                     val + " to a boolean value.");
         }
      }
      
      val = configuration.get(HTTPMetadataConstants.USE_REMOTING_CONTENT_TYPE);
      if (val instanceof String)
      {
         useRemotingContentType = Boolean.valueOf((String) val).booleanValue();
      }
      else if (val != null)
      {
         log.warn(HTTPMetadataConstants.USE_REMOTING_CONTENT_TYPE + " value should be a String: " + val);
      }
      log.debug(this + " useRemotingContentType: " + useRemotingContentType);
   }

   public void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
   {
      Map metadata = new HashMap();

      Enumeration enumer = request.getHeaderNames();
      while(enumer.hasMoreElements())
      {
         Object obj = enumer.nextElement();
         String headerKey = (String) obj;
         String headerValue = request.getHeader(headerKey);
         metadata.put(headerKey, headerValue);
      }

      Map urlParams = request.getParameterMap();
      if (unwrapSingletonArrays)
      {
         Iterator it = urlParams.keySet().iterator();
         while (it.hasNext())
         {
            Object key = it.next();
            Object value = urlParams.get(key);
            String[] valueArray = (String[]) value;
            if (valueArray.length == 1)
            {
               value = valueArray[0];
            }
            metadata.put(key, value);
         }
      }
      else
      {
         metadata.putAll(urlParams);
      }
      
      if(log.isTraceEnabled())
      {
         log.trace("metadata:");
         Iterator it = metadata.keySet().iterator();
         while (it.hasNext())
         {
            Object key = it.next();
            log.trace("  " + key + ": " + metadata.get(key));
         }
      }
      
      // UnMarshaller may not be an HTTPUnMarshaller, in which case it
      // can ignore this parameter.
      Object o = configuration.get(HTTPUnMarshaller.PRESERVE_LINES);
      if (o != null)
      {
         if (o instanceof String[])
         {
            metadata.put(HTTPUnMarshaller.PRESERVE_LINES, ((String[]) o)[0]);
         }
         else
         {
            metadata.put(HTTPUnMarshaller.PRESERVE_LINES, o);
         }
      }

      String requestContentType = request.getContentType();


      try
      {
         Object invocationResponse = null;

         ServletInputStream inputStream = request.getInputStream();
         UnMarshaller unmarshaller = MarshalFactory.getUnMarshaller(HTTPUnMarshaller.DATATYPE, getSerializationType());
         Object obj = null;
         if (unmarshaller instanceof VersionedUnMarshaller)
            obj = ((VersionedUnMarshaller)unmarshaller).read(inputStream, metadata, getVersion());
         else
            obj = unmarshaller.read(inputStream, metadata);
         inputStream.close();

         InvocationRequest invocationRequest = null;

         if(obj instanceof InvocationRequest)
         {
            invocationRequest = (InvocationRequest) obj;
         }
         else
         {
            if(WebUtil.isBinary(requestContentType))
            {
               invocationRequest = getInvocationRequest(metadata, obj);
            }
            else
            {
               invocationRequest = createNewInvocationRequest(metadata, obj);
            }
         }
         
         String remoteAddressString = request.getRemoteAddr();
         InetAddress remoteAddress = getAddressByName(remoteAddressString);
         Map requestPayload = invocationRequest.getRequestPayload();
         
         if (requestPayload == null)
         {
            requestPayload = new HashMap();
            invocationRequest.setRequestPayload(requestPayload);
         }
         
         requestPayload.put(Remoting.CLIENT_ADDRESS, remoteAddress);

         try
         {
            // call transport on the subclass, get the result to handback
            invocationResponse = invoke(invocationRequest);
         }
         catch(Throwable ex)
         {
            log.debug("Error thrown calling invoke on server invoker.", ex);
            invocationResponse = null;
            response.sendError(500, "Error processing invocation request.  " + ex.getMessage());
         }

         if(invocationResponse != null)
         {
            if (isInvalidContentType(requestContentType))
            {
               log.warn("Ignoring invalid content-type from client: " + requestContentType);
            }
            else
            {
               response.setContentType(requestContentType);
            }
            int iContentLength = getContentLength(invocationResponse);
            response.setContentLength(iContentLength);
            ServletOutputStream outputStream = response.getOutputStream();
            Marshaller marshaller = MarshalFactory.getMarshaller(HTTPMarshaller.DATATYPE, getSerializationType());
            if (marshaller instanceof VersionedMarshaller)
               ((VersionedMarshaller) marshaller).write(invocationResponse, outputStream, getVersion());
            else
               marshaller.write(invocationResponse, outputStream);
            outputStream.close();
         }

      }
      catch(ClassNotFoundException e)
      {
         log.error("Error processing invocation request due to class not being found.", e);
         response.sendError(500, "Error processing invocation request due to class not being found.  " + e.getMessage());

      }

   }

   public byte[] processRequest(HttpServletRequest request, byte[] requestByte,
                                HttpServletResponse response)
         throws ServletException, IOException
   {
      byte[] retval = new byte[0];
      
      // Check if client is HTTPClientInvoker
      boolean isRemotingUserAgent = false;
      String userAgent = request.getHeader(HTTPMetadataConstants.REMOTING_USER_AGENT);
      if (userAgent != null)
      {
         isRemotingUserAgent = userAgent.startsWith("JBossRemoting");
      }

      Map metadata = new HashMap();

      Enumeration enumer = request.getHeaderNames();
      while(enumer.hasMoreElements())
      {
         Object obj = enumer.nextElement();
         String headerKey = (String) obj;
         String headerValue = request.getHeader(headerKey);
         metadata.put(headerKey, headerValue);
      }

      Map urlParams = request.getParameterMap();
      if (unwrapSingletonArrays)
      {
         Iterator it = urlParams.keySet().iterator();
         while (it.hasNext())
         {
            Object key = it.next();
            Object value = urlParams.get(key);
            String[] valueArray = (String[]) value;
            if (valueArray.length == 1)
            {
               value = valueArray[0];
            }
            metadata.put(key, value);
         }
      }
      else
      {
         metadata.putAll(urlParams);
      }

      metadata.put(HTTPMetadataConstants.METHODTYPE, request.getMethod());
      
      // UnMarshaller may not be an HTTPUnMarshaller, in which case it
      // can ignore this parameter.
      Object o = configuration.get(HTTPUnMarshaller.PRESERVE_LINES);
      if (o != null)
      {
         if (o instanceof String[])
         {
            metadata.put(HTTPUnMarshaller.PRESERVE_LINES, ((String[]) o)[0]);
         }
         else
         {
            metadata.put(HTTPUnMarshaller.PRESERVE_LINES, o);
         }
      }
      
      String path = request.getPathTranslated();
      if (path != null)
         metadata.put(HTTPMetadataConstants.PATH, path);

      metadata.put(HTTPMetadataConstants.USE_REMOTING_CONTENT_TYPE, Boolean.toString(useRemotingContentType));
      String remotingContentType = (String) metadata.get(HTTPMetadataConstants.REMOTING_CONTENT_TYPE);
      if (remotingContentType == null)
      {
         remotingContentType = (String) metadata.get(HTTPMetadataConstants.REMOTING_CONTENT_TYPE_LC);
      }
      
      String requestContentType = request.getContentType();


      try
      {
         InvocationRequest invocationRequest = null;
         Object responseObject = null;
         boolean isError = false;

         String method = request.getMethod();
         if (method.equals("GET") || method.equals("HEAD") || (method.equals("OPTIONS") && request.getContentLength() <= 0))
         {
            invocationRequest = createNewInvocationRequest(metadata, null);
         }
         else
         {
            ServletInputStream inputStream = request.getInputStream();
            UnMarshaller unmarshaller = getUnMarshaller();
            Object obj = null;
            if (unmarshaller instanceof VersionedUnMarshaller)
               obj = ((VersionedUnMarshaller)unmarshaller).read(new ByteArrayInputStream(requestByte), metadata, getVersion());
            else
               obj = unmarshaller.read(new ByteArrayInputStream(requestByte), metadata);
            inputStream.close();

            if(obj instanceof InvocationRequest)
            {
               invocationRequest = (InvocationRequest) obj;

               Map requestMap = invocationRequest.getRequestPayload();
               if (requestMap == null)
               {
                  invocationRequest.setRequestPayload(metadata);
               }
               else
               {
                  requestMap.putAll(metadata);
               }
            }
            else
            {
               if((useRemotingContentType && HTTPMetadataConstants.REMOTING_CONTENT_TYPE_NON_STRING.equalsIgnoreCase(remotingContentType))
                  || (!useRemotingContentType && WebUtil.isBinary(requestContentType)))
               {
                  invocationRequest = getInvocationRequest(metadata, obj);
               }
               else
               {
                  invocationRequest = createNewInvocationRequest(metadata, obj);
               }
            }
         }

         String remoteAddressString = request.getRemoteAddr();
         InetAddress remoteAddress = getAddressByName(remoteAddressString);
         Map requestPayload = invocationRequest.getRequestPayload();
         
         if (requestPayload == null)
         {
            requestPayload = new HashMap();
            invocationRequest.setRequestPayload(requestPayload);
         }
         
         requestPayload.put(Remoting.CLIENT_ADDRESS, remoteAddress);
         
         
         try
         {
            // call transport on the subclass, get the result to handback
            responseObject = invoke(invocationRequest);
         }
         catch(Throwable ex)
         {
            log.debug("Error thrown calling invoke on server invoker.", ex);
            
            if (checkForNoExceptionReturn(metadata))
            {
               log.trace("Returning error message instead of Exception");
               response.addHeader(HTTPMetadataConstants.REMOTING_CONTENT_TYPE, HTTPMetadataConstants.REMOTING_CONTENT_TYPE_STRING); 
               response.sendError(500, "Error occurred processing invocation request. ");
               return retval;
            }
            else
            {
               responseObject = ex;
               isError = true;
            }
         }

         int status = 200;
         if(responseObject != null)
         {
            if(isError)
            {
               status = 500;
            }
         }
         else
         {
            if (!isRemotingUserAgent || "HEAD".equals(request.getMethod()))
            {
               status = 204;
            }
         }

         // extract response code/message if exists
         Map responseMap = invocationRequest.getReturnPayload();
         if(responseMap != null)
         {
            Integer handlerStatus = (Integer) responseMap.remove(HTTPMetadataConstants.RESPONSE_CODE);
            if(handlerStatus != null)
            {
               status = handlerStatus.intValue();
            }

            // add any response map headers
            Set entries = responseMap.entrySet();
            Iterator itr = entries.iterator();
            while(itr.hasNext())
            {
               Map.Entry entry = (Map.Entry)itr.next();
               response.addHeader(entry.getKey().toString(), entry.getValue().toString());
            }
         }



         // can't set message anymore as is deprecated
         response.setStatus(status);
         
         if (isRemotingUserAgent && !(invocationRequest instanceof CreatedInvocationRequest))
         {
            responseObject = new InvocationResponse(invocationRequest.getSessionId(),
                                                    responseObject, isError, responseMap);
         }

         if(responseObject != null)
         {
            String responseContentType = null;
            if (responseMap != null)
            {
               responseContentType = (String) responseMap.get("Content-Type");
            }
            
            if (responseContentType != null)
            {
               if (isInvalidContentType(responseContentType))
               {
                  log.warn("Ignoring invalid content-type from ServerInvocationHandler: " + responseContentType);
                  responseContentType = WebUtil.getContentType(responseObject); 
               }
            }
            else
            {
               responseContentType = WebUtil.getContentType(responseObject); 
            }
            response.setContentType(responseContentType);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Marshaller marshaller = getMarshaller();
            if (marshaller instanceof VersionedMarshaller)
               ((VersionedMarshaller) marshaller).write(responseObject, outputStream, getVersion());
            else
               marshaller.write(responseObject, outputStream);
            retval = outputStream.toByteArray();
            response.setContentLength(retval.length);
         }
         
         if (responseObject instanceof String)
         {
            response.addHeader(HTTPMetadataConstants.REMOTING_CONTENT_TYPE, HTTPMetadataConstants.REMOTING_CONTENT_TYPE_STRING);
         }
         else
         {
            response.addHeader(HTTPMetadataConstants.REMOTING_CONTENT_TYPE, HTTPMetadataConstants.REMOTING_CONTENT_TYPE_NON_STRING);        
         }
      }
      catch(ClassNotFoundException e)
      {
         log.error("Error processing invocation request due to class not being found.", e);
         response.sendError(500, "Error processing invocation request due to class not being found.  " + e.getMessage());
      }

      return retval;
   }
   
   static private boolean isInvalidContentType(String contentType)
   {
      return contentType.indexOf('\n') + contentType.indexOf('\r') > -2;
   }
   
   private boolean checkForNoExceptionReturn(Map headers)
   {
      boolean flag = false;

      if(headers != null)
      {
         Object val = headers.get(HTTPMetadataConstants.DONT_RETURN_EXCEPTION);
         if (val != null)
         {
            if (val instanceof String)
            {
               flag = Boolean.valueOf((String) val).booleanValue();
            }
            else if (val instanceof String[])
            {
               String param = ((String[]) val)[0];
               flag = Boolean.valueOf(param).booleanValue();
            }
         }
      }
      
      return flag;
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