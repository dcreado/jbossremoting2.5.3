/*
 *  Copyright 1999-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


package org.jboss.remoting.transport.coyote;

import org.apache.coyote.ActionCode;
import org.apache.coyote.Adapter;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.Request;
import org.apache.coyote.Response;
import org.apache.tomcat.util.buf.B2CConverter;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.CharChunk;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.http.MimeHeaders;
import org.apache.tomcat.util.net.SocketStatus;
import org.jboss.remoting.Home;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvocationResponse;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.Remoting;
import org.jboss.remoting.Version;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.marshal.VersionedMarshaller;
import org.jboss.remoting.marshal.VersionedUnMarshaller;
import org.jboss.remoting.marshal.http.HTTPUnMarshaller;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.transport.coyote.ssl.RemotingSSLImplementation;
import org.jboss.remoting.transport.coyote.ssl.RemotingServerSocketFactory;
import org.jboss.remoting.transport.http.HTTPMetadataConstants;
import org.jboss.remoting.transport.web.WebServerInvoker;
import org.jboss.remoting.transport.web.WebUtil;
import org.jboss.remoting.util.SecurityUtility;
import org.jboss.logging.Logger;

import javax.net.ServerSocketFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This is the stand alone http server invoker which acts basically as a web server.
 * Server invoker implementation based on http protocol.  Is basically a stand alone http server whose request are
 * forwared to the invocation handler and responses from invocation handler are sent back to caller as http response.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 */

/**
 * Some of the code in this class was pulled from org.apache.coyote.tomcat4.CoyoteAdapter
 * and hence will maintain the Apache License (and author credit from original source).
 */
public class CoyoteInvoker extends WebServerInvoker implements Adapter
{
   private static final Logger log = Logger.getLogger(CoyoteInvoker.class);

   /** Indicates if input was raw or an InvocationRequest */
   protected static ThreadLocal receivedInvocationRequest = new ThreadLocal();
   protected static final Boolean FALSE = new Boolean(false);
   protected static final Boolean TRUE = new Boolean(true);

   private boolean running = false;

//   protected ProtocolHandler protocolHandler = null;
   protected List protocolHandlers = new ArrayList();

   protected String URIEncoding = null;
   
   protected String useRemotingContentType = "false";


   public CoyoteInvoker(InvokerLocator locator)
   {
      super(locator);
   }

   public CoyoteInvoker(InvokerLocator locator, Map configuration)
   {
      super(locator, configuration);
   }

   protected void setup() throws Exception
   {

      super.setup();

      Map config = getConfiguration();

      // Test APR support
      boolean apr = false;
      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               String methodName = "initialize";
               Class paramTypes[] = new Class[1];
               paramTypes[0] = String.class;
               Object paramValues[] = new Object[1];
               paramValues[0] = null;
               String className = "org.apache.tomcat.jni.Library";
               Method method = Class.forName(className).getMethod(methodName, paramTypes);
               method.invoke(null, paramValues);
               return null;
            }
         });
         apr = true;
      }
      catch (PrivilegedActionException e)
      {
         // Ignore.
         log.trace("", e.getCause());
      }

      // Instantiate the associated HTTP protocol handler
      String protocolHandlerClassName = null;
      Object value = config.get("protocolHandlerClassName");
      if(value != null)
      {
         protocolHandlerClassName = String.valueOf(value);
      }
      else
      {
         if(apr)
         {
            protocolHandlerClassName = "org.apache.coyote.http11.Http11AprProtocol";
         }
         else
         {
            protocolHandlerClassName = "org.apache.coyote.http11.Http11Protocol";
         }
      }
      log.info("Using " + protocolHandlerClassName + " for http (coyote) invoker protocol handler.");

      Class clazz = null;
      try
      {
         clazz = (Class) forName(protocolHandlerClassName);
      }
      catch (ClassNotFoundException e)
      {
         log.error("Protocol handler class instatiation failed: protocolHandlerClassName", e);
         return;
      }
      
      ProtocolHandler protocolHandler = null;
      for (int i = 0; i < connectHomes.size(); i++)
      {
         try
         {
            protocolHandler = (ProtocolHandler) clazz.newInstance();
         }
         catch(Exception e)
         {
            log.error("Protocol handler instantiation failed", e);
            return;
         }
         protocolHandler.setAdapter(this);

         // Pass all attributes to the protocol handler
         Iterator keys = config.keySet().iterator();
         while(keys.hasNext())
         {
            String key = (String) keys.next();
            Object obj = config.get(key);
            if (obj instanceof String)
            {
               String val = (String) obj;
               setProperty(protocolHandler, key, val);
            }
         }

         // need to convert standard remoting timeout config to tomcat timeout
         String timeoutValue = (String)config.get(TIMEOUT);
         if(timeoutValue != null)
         {
            setProperty(protocolHandler, "connectionTimeout", timeoutValue);
         }

         // Configuration of URI encoding
         value = config.get("URIEncoding");
         if(value != null)
         {
            URIEncoding = String.valueOf(value);
         }
         
         protocolHandlers.add(protocolHandler);
         
         value = config.get(HTTPMetadataConstants.USE_REMOTING_CONTENT_TYPE);
         if (value instanceof String)
         {
            useRemotingContentType = (String) value;
         }
         else if (value != null)
         {
            log.warn(HTTPMetadataConstants.USE_REMOTING_CONTENT_TYPE + " value should be a String: " + value);
         }
         log.debug(this + " useRemotingContentType: " + useRemotingContentType);
      }
   }

   protected ServerSocketFactory getDefaultServerSocketFactory() throws IOException
   {
      /**
       * Returning a null here as if has not already been set previously
       * via config in ServerInvoker, then want to return null so that
       * will use the default within tomcat (and not override with own default).
       */

      if ("https".equals(locator.getProtocol()))
      {
         SSLSocketBuilder builder = new SSLSocketBuilder(configuration);
         builder.setUseSSLServerSocketFactory(false);
         try
         {
            return builder.createSSLServerSocketFactory();
         }
         catch (IOException e)
         {
            log.debug("unable to create server socket factory", e);
            throw e;
         }
      }
      else
         return null;
   }

   public void start() throws IOException
   {
      if(!running)
      {
         for (int i = 0; i < protocolHandlers.size(); i++)
         {
            try
            {
               final ProtocolHandler protocolHandler = (ProtocolHandler) protocolHandlers.get(i);
               Home home = (Home) getHomes().get(i);
               setProperty(protocolHandler, "address", home.host);
               setProperty(protocolHandler, "port", "" + home.port);

               //TODO: -TME - Should not have to hard set this every time.  Should
               // be a way to figure out if this is needed or not.

               // Need to set the MBeanServer to use since there is no direct way to do it.
               setProperty(protocolHandler, "locator", getLocator().getLocatorURI());
               RemotingSSLImplementation.setMBeanServer(getLocator().getLocatorURI(), getMBeanServer());

               ServerSocketFactory svrSocketFactory = getServerSocketFactory();
               if(svrSocketFactory != null)
               {
                  RemotingServerSocketFactory.setServerSocketFactory(getLocator().getLocatorURI(), svrSocketFactory);
                  setProperty(protocolHandler, "SocketFactory", RemotingServerSocketFactory.class.getName());
               }

               try
               {
                  AccessController.doPrivileged( new PrivilegedExceptionAction()
                  {
                     public Object run() throws Exception
                     {
                        protocolHandler.init();
                        protocolHandler.start();
                        return null;
                     }
                  });
               }
               catch (PrivilegedActionException e)
               {
                  throw (Exception) e.getCause();
               }

               running = true;

            }
            catch(Exception e)
            {
               log.debug("Error starting protocol handler.  Bind port: " + getServerBindPort() + ", bind address: " + getServerBindAddress(), e);
               throw new IOException("" + e.getMessage());
            }
         }
      }
      super.start();
   }

   /**
    * Service method.
    */
   public void service(org.apache.coyote.Request req,
                       org.apache.coyote.Response res)
         throws Exception
   {

      RequestMap request = (RequestMap) req.getNote(1);
      ResponseMap response = (ResponseMap) res.getNote(1);

      if(request == null)
      {

         // Create objects
         request = new RequestMap();
         request.setCoyoteRequest(req);
         response = new ResponseMap();
         response.setCoyoteResponse(res);

         // Set as notes
         req.setNote(1, request);
         res.setNote(1, response);

         // Set query string encoding
         // FIMXE?: req.getParameters().setQueryStringEncoding(protocolHandler.getAttribute("URIEncoding"));

      }
      else
      {
         response.clear();
         request.clear();
      }

      try
      {

         if(postParseRequest(req, request, res, response))
         {
            populateRequestMetadata(request, req);

            Object responseObject = null;
            boolean isError = false;

            int version = getVersion(request);

            // Check if client is HTTPClientInvoker
            boolean isRemotingUserAgent = false;
            Object userAgentObj = request.get(HTTPMetadataConstants.REMOTING_USER_AGENT);
            if (userAgentObj != null)
            {
               String userAgent = (String) userAgentObj;
               isRemotingUserAgent = userAgent.startsWith("JBossRemoting");
            }

            InvocationRequest invocationRequest = versionedRead(req, request, response, version);

            if (invocationRequest.getRequestPayload() == null)
               invocationRequest.setRequestPayload(new HashMap());
            
            MessageBytes remoteAddressMB = req.remoteAddr();
            if (remoteAddressMB != null)
            {
               String remoteAddressString = remoteAddressMB.toString();
               InetAddress remoteAddress = getAddressByName(remoteAddressString);
               invocationRequest.getRequestPayload().put(Remoting.CLIENT_ADDRESS, remoteAddress);  
            }
            else
            {
               log.debug("unable to retrieve client address from coyote transport layer");
            }
            
            
            // FIXME: OPTIONS method handling ?
            try
            {
               // call transport on the subclass, get the result to handback
               responseObject = invoke(invocationRequest);
            }
            catch(Throwable ex)
            {
               log.debug("Error thrown calling invoke on server invoker.", ex);
               responseObject = ex;
               isError = true;
            }

            //Start with response code of 204 (no content), then if is a return from handler, change to 200 (ok)
            int status;
            String message = "";

            if(responseObject != null)
            {
               if(isError)
               {
                  status = 500;
                  message = "JBoss Remoting: Error occurred within target application.";
               }
               else
               {
                  status = 200;
                  message = "OK";
               }
            }
            else
            {
               if (isRemotingUserAgent && !req.method().equals("HEAD"))
               {
                  status = 200;
                  message = "OK";
               }
               else
               {
                  status = 204;
                  message = "No Content";
               }
            }

            // extract response code/message if exists
            Map responseMap = invocationRequest.getReturnPayload();
            if(responseMap != null)
            {
               Integer handlerStatus = (Integer) responseMap.get(HTTPMetadataConstants.RESPONSE_CODE);
               if(handlerStatus != null)
               {
                  status = handlerStatus.intValue();
               }
               String handlerMessage = (String) responseMap.get(HTTPMetadataConstants.RESPONSE_CODE_MESSAGE);
               if(handlerMessage != null)
               {
                  message = handlerMessage;
               }
            }
            res.setStatus(status);
            res.setMessage(message);

            if (isRemotingUserAgent && ((Boolean)receivedInvocationRequest.get()).booleanValue())
            {
               responseMap = ((ResponseMap) responseMap).getMap();
               responseObject = new InvocationResponse(invocationRequest.getSessionId(),
                                                       responseObject, isError, responseMap);
            }

            if(responseObject != null)
            {
               versionedWrite(version, responseObject, req, res, response);
            }

         }

         response.outputBuffer.close();

         req.action(ActionCode.ACTION_POST_REQUEST, null);

      }
      catch (ClientAbortException e)
      {
         log.debug("Client didn't wait", e);
      }
      catch(IOException e)
      {
         log.error("Error processing request", e);
      }
      catch(Throwable t)
      {
         log.error("Service error", t);
      }
      finally
      {
         // Recycle the wrapper request and response
         request.recycle();
         response.recycle();
      }

   }

   private void addLeaseInfo(ResponseMap response)
   {
      boolean leaseManagement = isLeaseActivated();
      response.put("LEASING_ENABLED", new Boolean(leaseManagement));

      if(leaseManagement)
      {
         long leasePeriod = getLeasePeriod();
         response.put("LEASE_PERIOD", new Long(leasePeriod));
      }
   }

   private void versionedWrite(int version, Object responseObject, Request req, org.apache.coyote.Response res, ResponseMap response)
         throws IOException
   {
      switch (version)
      {
         case Version.VERSION_1:
         case Version.VERSION_2:
         case Version.VERSION_2_2:
         {
            String responseContentType = (String) response.get("Content-Type");
            if (responseContentType != null)
            {
               if (isInvalidContentType(responseContentType))
               {
                  log.warn("Ignoring invalid content-type from ServerInvocationHandler: " + responseContentType);
                  if (responseObject == null)
                  {
                     responseContentType = req.getContentType();
                     if (isInvalidContentType(responseContentType))
                     {
                        log.warn("Ignoring invalid content-type from request: " + responseContentType);
                        responseContentType = WebUtil.getContentType(responseObject); 
                     }
                  }
                  else
                  {
                     responseContentType = WebUtil.getContentType(responseObject); 
                  }
               }
            }
            else
            {
               if (responseObject == null)
               {
                  responseContentType = req.getContentType();
                  if (isInvalidContentType(responseContentType))
                  {
                     log.warn("Ignoring invalid content-type from request: " + responseContentType);
                     responseContentType = WebUtil.getContentType(responseObject); 
                  }
               }
               else
               {
                  responseContentType = WebUtil.getContentType(responseObject); 
               }
            }
            res.setContentType(responseContentType);
            
            if (responseObject instanceof String)
            {
               res.addHeader(HTTPMetadataConstants.REMOTING_CONTENT_TYPE, HTTPMetadataConstants.REMOTING_CONTENT_TYPE_STRING);
            }
            else
            {
               res.addHeader(HTTPMetadataConstants.REMOTING_CONTENT_TYPE, HTTPMetadataConstants.REMOTING_CONTENT_TYPE_NON_STRING);        
            }
            
            Marshaller marshaller = getMarshaller();
            if (marshaller instanceof VersionedMarshaller)
               ((VersionedMarshaller) marshaller).write(responseObject, response.getOutputStream(), version);
            else
               marshaller.write(responseObject, response.getOutputStream());
            return;
         }
         default:
         {
            throw new IOException("Can not send response due to version (" + version + ") not being supported.  Supported versions: " + Version.VERSION_1 + ", " + Version.VERSION_2 + ", " + Version.VERSION_2_2);
         }
      }
   }

   private InvocationRequest versionedRead(Request req, RequestMap request, ResponseMap response, int version)
         throws IOException, ClassNotFoundException
   {
      switch (version)
      {
         case Version.VERSION_1:
         case Version.VERSION_2:
         case Version.VERSION_2_2:
         {
            // UnMarshaller may not be an HTTPUnMarshaller, in which case it
            // can ignore this parameter.
            Object o = configuration.get(HTTPUnMarshaller.PRESERVE_LINES);
            if (o != null)
            {
               request.put(HTTPUnMarshaller.PRESERVE_LINES, o);
            }

            receivedInvocationRequest.set(FALSE);
            InvocationRequest invocationRequest = null;
            MessageBytes method = req.method();
            if (method.equals("GET") || method.equals("HEAD")
                  || (method.equals("OPTIONS") && req.getContentLength() <= 0))
            {
               invocationRequest = createNewInvocationRequest(request, response, null);
            } else
            {
               // must be POST or PUT
               UnMarshaller unmarshaller = getUnMarshaller();
               request.put(HTTPMetadataConstants.USE_REMOTING_CONTENT_TYPE, useRemotingContentType);
               Object obj = null;
               if (unmarshaller instanceof VersionedUnMarshaller)
                  obj = ((VersionedUnMarshaller)unmarshaller).read(request.getInputStream(), request, version);
               else
                  obj = unmarshaller.read(request.getInputStream(), request);
               if (obj instanceof InvocationRequest)
               {
                  receivedInvocationRequest.set(TRUE);
                  invocationRequest = (InvocationRequest) obj;
                  if (invocationRequest.getReturnPayload() == null)
                  {
                     // need to create a return payload map, so can be populated with metadata
                     invocationRequest.setReturnPayload(response);
                  }
                  Map requestPayloadMap = invocationRequest.getRequestPayload();
                  if (requestPayloadMap != null)
                  {
                     request.putAll(requestPayloadMap);
                  }
                  invocationRequest.setRequestPayload(request);
               } else
               {
                  invocationRequest = createNewInvocationRequest(request, response, obj);
               }
            }

            return invocationRequest;
         }

         default:
         {
            throw new IOException("Can not processes request due to incorrect version (" + version + ").  Can only process versions: " + Version.VERSION_1 + ", " + Version.VERSION_2 + ", " + Version.VERSION_2_2);
         }
      }
   }

   private int getVersion(RequestMap request)
   {
      int version = Version.VERSION_1; // going to default to old version
      Object versionObj = request.get(HTTPMetadataConstants.REMOTING_VERSION_HEADER);
      if (versionObj != null)
      {
         String versionString = (String) versionObj;
         try
         {
            version = Integer.parseInt(versionString);
         } catch (NumberFormatException e)
         {
            log.error("Can not processes remoting version of " + versionString + " as is not a number.");
         }
      }
      return version;
   }

   private void populateRequestMetadata(RequestMap metadata, Request req)
   {
      final MimeHeaders headers = req.getMimeHeaders();
      Enumeration nameEnum = null;
      
      if (SecurityUtility.skipAccessControl())
      {
         nameEnum = headers.names();
      }
      else
      {
         nameEnum = (Enumeration)AccessController.doPrivileged( new PrivilegedAction()
         {
            public Object run()
            {
               return headers.names();
            }}
         );
      }

      while (nameEnum.hasMoreElements())
      {
         Object nameObj = nameEnum.nextElement();
         if (nameObj instanceof String)
         {
            Object valueObj = headers.getHeader((String) nameObj);
            metadata.put(nameObj, valueObj);
         }
      }

      metadata.put(HTTPMetadataConstants.METHODTYPE, req.method().getString());
      metadata.put(HTTPMetadataConstants.PATH, req.requestURI().getString());
      metadata.put(HTTPMetadataConstants.QUERY, req.query().toString());
      metadata.put(HTTPMetadataConstants.HTTPVERSION, req.protocol().getString());

   }


   protected InvocationRequest createNewInvocationRequest(RequestMap requestMap, ResponseMap responseMap,
                                                          Object payload)
   {
      // will try to use the same session id if possible to track
      String sessionId = getSessionId(requestMap);
      String subSystem = (String) requestMap.get(HEADER_SUBSYSTEM);

      InvocationRequest request = null;

      boolean isLeaseQueury = checkForLeaseQuery(requestMap);
      if(isLeaseQueury)
      {
         addLeaseInfo(responseMap);
         request = new InvocationRequest(sessionId, subSystem, "$PING$", null, responseMap, null);
      }
      else
      {
         request = new InvocationRequest(sessionId, subSystem, payload,
                                                        requestMap, responseMap, null);
      }
      return request;
   }

   private boolean checkForLeaseQuery(RequestMap headers)
   {
      boolean isLeaseQuery = false;

         if(headers != null)
         {
            Object val = headers.get(HTTPMetadataConstants.REMOTING_LEASE_QUERY);
            if(val != null && val instanceof String)
            {
               isLeaseQuery = Boolean.valueOf((String)val).booleanValue();
            }
         }
      return isLeaseQuery;
   }

   /**
    * Parse additional request parameters.
    */
   protected boolean postParseRequest(org.apache.coyote.Request req,
                                      RequestMap request,
                                      org.apache.coyote.Response res,
                                      ResponseMap response)
         throws Exception
   {

      // URI decoding
      MessageBytes decodedURI = req.decodedURI();
      decodedURI.duplicate(req.requestURI());

      if(decodedURI.getType() == MessageBytes.T_BYTES)
      {
         // %xx decoding of the URL
         try
         {
            req.getURLDecoder().convert(decodedURI, false);
         }
         catch(IOException ioe)
         {
            res.setStatus(400);
            res.setMessage("Invalid URI");
            throw ioe;
         }
         // Normalization
         if(!normalize(req.decodedURI()))
         {
            res.setStatus(400);
            res.setMessage("Invalid URI");
            return false;
         }
         // Character decoding
         convertURI(decodedURI, request);
      }
      else
      {
         // The URL is chars or String, and has been sent using an in-memory
         // protocol handler, we have to assume the URL has been properly
         // decoded already
         decodedURI.toChars();
      }

      return true;

   }


   /**
    * Character conversion of the URI.
    */
   protected void convertURI(MessageBytes uri, RequestMap request)
         throws Exception
   {

      ByteChunk bc = uri.getByteChunk();
      CharChunk cc = uri.getCharChunk();
      cc.allocate(bc.getLength(), -1);

      String enc = URIEncoding;
      if(enc != null)
      {
         B2CConverter conv = request.getURIConverter();
         try
         {
            if(conv == null)
            {
               conv = new B2CConverter(enc);
               request.setURIConverter(conv);
            }
            else
            {
               conv.recycle();
            }
         }
         catch(IOException e)
         {
            // Ignore
            log.error("Invalid URI encoding; using HTTP default");
            URIEncoding = null;
         }
         if(conv != null)
         {
            try
            {
               conv.convert(bc, cc);
               uri.setChars(cc.getBuffer(), cc.getStart(),
                            cc.getLength());
               return;
            }
            catch(IOException e)
            {
               log.error("Invalid URI character encoding; trying ascii");
               cc.recycle();
            }
         }
      }

      // Default encoding: fast conversion
      byte[] bbuf = bc.getBuffer();
      char[] cbuf = cc.getBuffer();
      int start = bc.getStart();
      for(int i = 0; i < bc.getLength(); i++)
      {
         cbuf[i] = (char) (bbuf[i + start] & 0xff);
      }
      uri.setChars(cbuf, 0, bc.getLength());

   }


   /**
    * Character conversion of the a US-ASCII MessageBytes.
    */
   protected void convertMB(MessageBytes mb)
   {

      // This is of course only meaningful for bytes
      if(mb.getType() != MessageBytes.T_BYTES)
      {
         return;
      }

      ByteChunk bc = mb.getByteChunk();
      CharChunk cc = mb.getCharChunk();
      cc.allocate(bc.getLength(), -1);

      // Default encoding: fast conversion
      byte[] bbuf = bc.getBuffer();
      char[] cbuf = cc.getBuffer();
      int start = bc.getStart();
      for(int i = 0; i < bc.getLength(); i++)
      {
         cbuf[i] = (char) (bbuf[i + start] & 0xff);
      }
      mb.setChars(cbuf, 0, bc.getLength());

   }


   /**
    * Normalize URI.
    * <p/>
    * This method normalizes "\", "//", "/./" and "/../". This method will
    * return false when trying to go above the root, or if the URI contains
    * a null byte.
    *
    * @param uriMB URI to be normalized
    */
   public static boolean normalize(MessageBytes uriMB)
   {

      ByteChunk uriBC = uriMB.getByteChunk();
      byte[] b = uriBC.getBytes();
      int start = uriBC.getStart();
      int end = uriBC.getEnd();
      
      // Expect request URI to be at least one character.
      if (start - end == 0)
      {
         return false;
      }
      
      // URL * is acceptable
      if((end - start == 1) && b[start] == (byte) '*')
      {
         return true;
      }

      int pos = 0;
      int index = 0;

      // Replace '\' with '/'
      // Check for null byte
      for(pos = start; pos < end; pos++)
      {
         if(b[pos] == (byte) '\\')
         {
            b[pos] = (byte) '/';
         }
         if(b[pos] == (byte) 0)
         {
            return false;
         }
      }

      // The URL must start with '/'
      if(b[start] != (byte) '/')
      {
         return false;
      }

      // Replace "//" with "/"
      for(pos = start; pos < (end - 1); pos++)
      {
         if(b[pos] == (byte) '/')
         {
            while((pos + 1 < end) && (b[pos + 1] == (byte) '/'))
            {
               copyBytes(b, pos, pos + 1, end - pos - 1);
               end--;
            }
         }
      }

      // If the URI ends with "/." or "/..", then we append an extra "/"
      // Note: It is possible to extend the URI by 1 without any side effect
      // as the next character is a non-significant WS.
      if(((end - start) >= 2) && (b[end - 1] == (byte) '.'))
      {
         if((b[end - 2] == (byte) '/')
            || ((b[end - 2] == (byte) '.')
                && (b[end - 3] == (byte) '/')))
         {
            b[end] = (byte) '/';
            end++;
         }
      }

      uriBC.setEnd(end);

      index = 0;

      // Resolve occurrences of "/./" in the normalized path
      while(true)
      {
         index = uriBC.indexOf("/./", 0, 3, index);
         if(index < 0)
         {
            break;
         }
         copyBytes(b, start + index, start + index + 2,
                   end - start - index - 2);
         end = end - 2;
         uriBC.setEnd(end);
      }

      index = 0;

      // Resolve occurrences of "/../" in the normalized path
      while(true)
      {
         index = uriBC.indexOf("/../", 0, 4, index);
         if(index < 0)
         {
            break;
         }
         // Prevent from going outside our context
         if(index == 0)
         {
            return false;
         }
         int index2 = -1;
         for(pos = start + index - 1; (pos >= 0) && (index2 < 0); pos --)
         {
            if(b[pos] == (byte) '/')
            {
               index2 = pos;
            }
         }
         copyBytes(b, start + index2, start + index + 3,
                   end - start - index - 3);
         end = end + index2 - index - 3;
         uriBC.setEnd(end);
         index = index2;
      }

      uriBC.setBytes(b, start, end);

      return true;

   }


   /**
    * Copy an array of bytes to a different position. Used during
    * normalization.
    */
   protected static void copyBytes(byte[] b, int dest, int src, int len)
   {
      for(int pos = 0; pos < len; pos++)
      {
         b[pos + dest] = b[pos + src];
      }
   }


   public void stop()
   {
      if(running)
      {
         running = false;

         if (protocolHandlers != null)
         {
            Iterator it = protocolHandlers.iterator();
            while (it.hasNext())
            {
               try
               {
                  ProtocolHandler protocolHandler = (ProtocolHandler) it.next();
                  protocolHandler.destroy();
               }
               catch(Exception e)
               {
                  log.error("Stop error", e);
               }
            }
         }
      }
      super.stop();

      log.debug("CoyoteInvoker stopped.");
   }

   /**
    * Find a method with the right name If found, call the method ( if param is
    * int or boolean we'll convert value to the right type before) - that means
    * you can have setDebug(1).
    */
   public static boolean setProperty(final Object o, String name, final String value)
   {
      String setter = "set" + capitalize(name);

      try
      {
         Method[] methods = null;
         
         if (SecurityUtility.skipAccessControl())
         {
            methods = o.getClass().getMethods();
         }
         else
         {
            methods = (Method[]) AccessController.doPrivileged( new PrivilegedAction()
            {
               public Object run()
               {
                  return o.getClass().getMethods();
               }
            });
         }
         
         Method setPropertyMethod = null;

         // First, the ideal case - a setFoo( String ) method
         for(int i = 0; i < methods.length; i++)
         {
            Class paramT[] = methods[i].getParameterTypes();
            if(setter.equals(methods[i].getName()) && paramT.length == 1
               && "java.lang.String".equals(paramT[0].getName()))
            {

               methods[i].invoke(o, new Object[]{value});
               return true;
            }
         }

         // Try a setFoo ( int ) or ( boolean )
         for(int i = 0; i < methods.length; i++)
         {
            boolean ok = true;
            if(setter.equals(methods[i].getName())
               && methods[i].getParameterTypes().length == 1)
            {

               // match - find the type and invoke it
               Class paramType = methods[i].getParameterTypes()[0];
               Object params[] = new Object[1];

               // Try a setFoo ( int )
               if("java.lang.Integer".equals(paramType.getName())
                  || "int".equals(paramType.getName()))
               {
                  try
                  {
                     params[0] = new Integer(value);
                  }
                  catch(NumberFormatException ex)
                  {
                     ok = false;
                  }
                  // Try a setFoo ( long )
               }
               else if("java.lang.Long".equals(paramType.getName())
                       || "long".equals(paramType.getName()))
               {
                  try
                  {
                     params[0] = new Long(value);
                  }
                  catch(NumberFormatException ex)
                  {
                     ok = false;
                  }

                  // Try a setFoo ( boolean )
               }
               else if("java.lang.Boolean".equals(paramType.getName())
                       || "boolean".equals(paramType.getName()))
               {
                  params[0] = new Boolean(value);

                  // Try a setFoo ( InetAddress )
               }
               else if("java.net.InetAddress".equals(paramType.getName()))
               {
                  try
                  {
                     params[0] = getAddressByName(value);
                  }
                  catch(UnknownHostException exc)
                  {
                     ok = false;
                  }

                  // Unknown type
               }

               if(ok)
               {
                  methods[i].invoke(o, params);
                  return true;
               }
            }

            // save "setProperty" for later
            if("setProperty".equals(methods[i].getName()))
            {
               setPropertyMethod = methods[i];
            }
         }

         // Ok, no setXXX found, try a setProperty("name", "value")
         if(setPropertyMethod != null)
         {
            Object params[] = new Object[2];
            params[0] = name;
            params[1] = value;
            setPropertyMethod.invoke(o, params);
            return true;
         }

      }
      catch(Exception e)
      {
         return false;
      }
      return false;
   }

   /**
    * Reverse of Introspector.decapitalize
    */
   public static String capitalize(String name)
   {
      if(name == null || name.length() == 0)
      {
         return name;
      }
      char chars[] = name.toCharArray();
      chars[0] = Character.toUpperCase(chars[0]);
      return new String(chars);
   }

   
   /**
    * Necessary for implementation of org.apache.coyote.Adapter interface.
    * Body is vacuous because event() is only used in comet mode.
    */
   public boolean event(Request arg0, Response arg1, SocketStatus arg2) throws Exception
   {
      return true;
   }
   
   static private boolean isInvalidContentType(String contentType)
   {
      return contentType.indexOf('\n') + contentType.indexOf('\r') > -2;
   }
   
   static private Object forName(final String className) throws ClassNotFoundException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return Class.forName(className);
      }
      
      try
      {
         return  AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return Class.forName(className);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (ClassNotFoundException) e.getCause();
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
