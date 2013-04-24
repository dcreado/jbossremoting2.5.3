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

package org.jboss.remoting.transport.http;

import org.jboss.logging.Logger;
import org.jboss.remoting.CannotConnectException;
import org.jboss.remoting.ConnectionFailedException;
import org.jboss.remoting.Home;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvocationResponse;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.RemoteClientInvoker;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.Version;
import org.jboss.remoting.marshal.MarshalFactory;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.marshal.VersionedMarshaller;
import org.jboss.remoting.marshal.VersionedUnMarshaller;
import org.jboss.remoting.marshal.compress.CompressingUnMarshaller;
import org.jboss.remoting.marshal.http.HTTPMarshaller;
import org.jboss.remoting.marshal.http.HTTPUnMarshaller;
import org.jboss.remoting.marshal.serializable.SerializableUnMarshaller;
import org.jboss.remoting.serialization.ClassLoaderUtility;
import org.jboss.remoting.transport.web.WebUtil;
import org.jboss.remoting.util.SecurityUtility;
import org.jboss.util.Base64;
import org.jboss.util.threadpool.BasicThreadPool;
import org.jboss.util.threadpool.BlockingMode;
import org.jboss.util.threadpool.RunnableTaskWrapper;
import org.jboss.util.threadpool.Task;
import org.jboss.util.threadpool.ThreadPool;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * HTTP client invoker.  Used for making http requests on http/servlet invoker.
 *
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class HTTPClientInvoker extends RemoteClientInvoker
{
   /**
    * Key for the configuration map that determines the threadpool size for 
    * simulated timeouts when using jdk 1.4.
    */
   public static final String MAX_NUM_TIMEOUT_THREADS = "maxNumTimeoutThreads";

   /**
    * Key for the configuration map that determines the queue size for simulated
    * timeout threadpool when using jdk 1.4.
    */
   public static final String MAX_TIMEOUT_QUEUE_SIZE = "maxTimeoutQueueSize";
   
   /**
    * Specifies the default number of work threads in the thread pool for 
    * simulating timeouts when using jdk 1.4.
    */
   public static final int MAX_NUM_TIMEOUT_THREADS_DEFAULT = 10;
   
   /**
    * Specifies the number of attempts to get a functioning connection
    * to the http server.  Defaults to 1.
    */
   public static final String NUMBER_OF_CALL_ATTEMPTS = "numberOfCallAttempts";
   
   /*
    * Specifies whether useHttpURLConnection(), upon receiving a null InputStream or ErrorStream,
    * should call the UnMarshaller.
    */
   public static final String UNMARSHAL_NULL_STREAM = "unmarshalNullStream";
   
   protected static final Logger log = Logger.getLogger(HTTPClientInvoker.class);
   
   protected boolean noThrowOnError;
   protected int numberOfCallAttempts = 1;
   protected boolean unmarshalNullStream = true;
   protected boolean useRemotingContentType = false;
   
   private Object timeoutThreadPoolLock = new Object();
   private ThreadPool timeoutThreadPool;

   public HTTPClientInvoker(InvokerLocator locator)
   {
      super(locator);
      configureParameters();
   }

   public HTTPClientInvoker(InvokerLocator locator, Map configuration)
   {
      super(locator, configuration);
      configureParameters();
   }

   /**
    * @param sessionId
    * @param invocation
    * @param marshaller
    * @return
    * @throws java.io.IOException
    * @throws org.jboss.remoting.ConnectionFailedException
    *
    */
   protected Object transport(String sessionId, final Object invocation, Map metadata,
                              final Marshaller marshaller, final UnMarshaller unmarshaller)
         throws IOException, ConnectionFailedException
   {
      // need to check the url and make sure it compatible protocol
      final String validatedUrl = validateURL(getLocator().getLocatorURI());

      if (metadata == null)
      {
         metadata = new HashMap();
      }

      final HttpURLConnection conn = createURLConnection(validatedUrl, metadata);
      
      int simulatedTimeout = getSimulatedTimeout(configuration, metadata, conn);
      
      if (simulatedTimeout <= 0)
      {
         return makeInvocation(conn, validatedUrl, invocation, metadata, marshaller, unmarshaller, true);
      }
      else
      {
         if (log.isTraceEnabled()) log.trace("using simulated timeout: " + simulatedTimeout);
         class Holder {public Object value;}
         final Holder resultHolder = new Holder();
         final Map finalMetadata = metadata;
         
         Runnable r = new Runnable()
         {
            public void run()
            {
               try
               {
                  resultHolder.value = useHttpURLConnection(conn, invocation, finalMetadata, marshaller, unmarshaller);
                  if (log.isTraceEnabled()) log.trace("result: " + resultHolder.value);
               }
               catch (Exception e)
               {
                  resultHolder.value = e;
                  if (log.isTraceEnabled()) log.trace("exception: " + e); 
               }
            }
         };
         
         // BasicThreadPool timeout mechanism depends on the interrupted status of
         // the running thread.
         Thread.interrupted();
         
         ThreadPool pool = getTimeoutThreadPool();
         WaitingTaskWrapper wrapper = new WaitingTaskWrapper(r, simulatedTimeout);
         if (log.isTraceEnabled()) log.trace("starting task in thread pool");
         pool.runTaskWrapper(wrapper);
         if (log.isTraceEnabled()) log.trace("task finished in thread pool");
         
         Object result = resultHolder.value;
         if (result == null)
         {
            if (log.isDebugEnabled()) log.debug("invocation timed out");
            Exception cause = new SocketTimeoutException("timed out");
            throw new CannotConnectException("Can not connect http client invoker.", cause);
         }
         else if (result instanceof IOException)
         {
            throw (IOException) result;
         }
         else if (result instanceof RuntimeException)
         {
            throw (RuntimeException) result;
         }
         else
         {
            if (log.isTraceEnabled()) log.trace("returning result: " + result);
            return result;
         }
      }
   }

   protected Object makeInvocation(HttpURLConnection conn, String url, Object invocation,
                                   Map metadata, Marshaller marshaller, UnMarshaller unmarshaller,
                                   boolean setTimeout)
   throws IOException
   {
      Throwable savedException = null;
      
      for (int i = 0; i < numberOfCallAttempts; i++)
      {
         try
         {
            Object o = useHttpURLConnection(conn, invocation, metadata, marshaller, unmarshaller);
            if (log.isTraceEnabled()) log.trace("result: " + o);
            return o;
         }
         catch (CannotConnectException e)
         {
            savedException = e.getCause();
            String suffix = (i < (numberOfCallAttempts - 1) ? ": will retry" : "");
            log.debug("Cannot connect on attempt " + (i + 1) + suffix);
            conn = createURLConnection(url, metadata);
            if (setTimeout)
            {
               getSimulatedTimeout(configuration, metadata, conn);
            }
         }
      }
      
      String msg = "Can not connect http client invoker after " + numberOfCallAttempts + " attempt(s)";
      throw new CannotConnectException(msg, savedException);
   }
   
   
   private Object useHttpURLConnection(HttpURLConnection conn, Object invocation, Map metadata,
                                       Marshaller marshaller, UnMarshaller unmarshaller) throws WebServerError
   {
      Object result = null;
      int responseCode = -1;

      try
      {
         setChunked(configuration, conn);

         // check to see if basic auth required
         String basicAuth = getBasicAuth(metadata);
         if (basicAuth != null)
         {
            conn.setRequestProperty("Authorization", basicAuth);
         }

         // check for ping request and process it now and return
         result = checkForLeasePing(conn,  invocation, metadata);
         if(result != null)
         {
            return result;
         }


         // Get the request method type
         boolean sendingData = true;
         String type = "POST";
         if (metadata != null)
         {
            type = (String) metadata.get("TYPE");
            if (type != null)
            {
               if ((!type.equals("POST") && !type.equals("PUT")))
               {
                  sendingData = false;
               }
            }
            else
            {
               type = "POST";
            }
         }
         else // need to check for content type and set metadata
         {
            metadata = new HashMap();
            Map header = new HashMap();
            header.put(HTTPMetadataConstants.CONTENTTYPE, WebUtil.getContentType(invocation));
            metadata.put("HEADER", header);
         }
         // Set request headers
         Map header = (Map) metadata.get("HEADER");
         if (header != null)
         {
            Set keys = header.keySet();
            Iterator itr = keys.iterator();
            while (itr.hasNext())
            {
               String key = (String) itr.next();
               String value = (String) header.get(key);
               log.debug("Setting request header with " + key + " : " + value);
               conn.setRequestProperty(key, value);
            }
         }
         else
         {
            conn.setRequestProperty(HTTPMetadataConstants.CONTENTTYPE, WebUtil.getContentType(invocation));
         }
         
         metadata.put(HTTPMetadataConstants.USE_REMOTING_CONTENT_TYPE, Boolean.toString(useRemotingContentType));

         // set the remoting version
         conn.setRequestProperty(HTTPMetadataConstants.REMOTING_VERSION_HEADER, new Integer(getVersion()).toString());
         // set the user agent
         conn.setRequestProperty(HTTPMetadataConstants.REMOTING_USER_AGENT, "JBossRemoting - " + Version.VERSION);

         if (sendingData)
         {
            //POST or PUT
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod(type);

            if (invocation instanceof String)
            {
               conn.setRequestProperty(HTTPMetadataConstants.REMOTING_CONTENT_TYPE, HTTPMetadataConstants.REMOTING_CONTENT_TYPE_STRING);
            }
            else
            {
               conn.setRequestProperty(HTTPMetadataConstants.REMOTING_CONTENT_TYPE, HTTPMetadataConstants.REMOTING_CONTENT_TYPE_NON_STRING);       
            }
            
            OutputStream stream = getOutputStream(conn);        
            if (marshaller instanceof VersionedMarshaller)
               ((VersionedMarshaller) marshaller).write(invocation, stream, getVersion());
            else
               marshaller.write(invocation, stream);
            responseCode = getResponseCode(conn);

            Map headers = conn.getHeaderFields();
            if (metadata == null)
            {
               metadata = new HashMap();
            }

            // sometimes I get headers with "null" keys (I don't know who's fault is it), so I need
            // to clean the header map, unless I want to get an NPE thrown by metadata.putAll()
            if (headers != null)
            {
               for(Iterator i = headers.entrySet().iterator(); i.hasNext(); )
               {
                  Map.Entry e = (Map.Entry)i.next();
                  if (e.getKey() != null)
                  {
                     metadata.put(e.getKey(), e.getValue());
                  }
               }
            }

            String responseMessage = getResponseMessage(conn);
            metadata.put(HTTPMetadataConstants.RESPONSE_CODE_MESSAGE, responseMessage);
            metadata.put(HTTPMetadataConstants.RESPONSE_CODE, new Integer(responseCode));
            metadata.put(HTTPMetadataConstants.RESPONSE_HEADERS, headers);

            InputStream is = (responseCode < 400) ? conn.getInputStream() : conn.getErrorStream();
            if (is != null || unmarshalNullStream)
            {
               result = readResponse(metadata, headers, unmarshaller, is);
            }
         }
         else
         {
            conn.setDoOutput(false);
            conn.setDoInput(true);
            conn.setRequestMethod(type);

            connect(conn);

            InputStream is = (getResponseCode(conn) < 400) ? conn.getInputStream() : conn.getErrorStream();
            Map headers = conn.getHeaderFields();

            if (is != null || unmarshalNullStream)
            {
               result = readResponse(null, headers, unmarshaller, is);
            }
            
            if (metadata == null)
            {
               metadata = new HashMap();
            }
            metadata.putAll(headers);
            String responseMessage = getResponseMessage(conn);
            metadata.put(HTTPMetadataConstants.RESPONSE_CODE_MESSAGE, responseMessage);
            responseCode = getResponseCode(conn);
            metadata.put(HTTPMetadataConstants.RESPONSE_CODE, new Integer(responseCode));
            metadata.put(HTTPMetadataConstants.RESPONSE_HEADERS, conn.getHeaderFields());
         }
      }
      catch (Exception e)
      {
         String message = "Can not connect http client invoker.";
         if (e.getMessage() != null)
            message += " " + e.getMessage() + ".";
 
         try
         {
            String responseMessage = getResponseMessage(conn);
            int code = getResponseCode(conn);
            message += " Response: " + responseMessage + "/" + code + ".";
         }
         catch (IOException e1)
         {
            log.debug("Unable to retrieve response message", e1);
         }
         throw new CannotConnectException(message, e);
      }

      // now check for error response and throw exception unless configured to not do so
      if(responseCode >= 400)
      {
         boolean doNotThrow = noThrowOnError;
         if(metadata != null)
         {
            Object configObj = metadata.get(HTTPMetadataConstants.NO_THROW_ON_ERROR);
            if(configObj != null && configObj instanceof String)
            {
               doNotThrow = Boolean.valueOf((String)configObj).booleanValue();
            }
         }

         if(doNotThrow)
         {
            if(result instanceof String)
            {
               // this is a html error page displayed by web server, need to conver to exception
               WebServerError ex = new WebServerError((String)result);
               return ex;
            }
            else if (result instanceof InvocationResponse)
            {
               return ((InvocationResponse) result).getResult();
            }
            else
            {
               return result;
            }
         }


         // if got here, wasn't configured to not throw exception, so will throw it.

         // In this case, MicroRemoteClientInvoker will throw the exception carried by
         // the InvocationResponse.
         if (result instanceof InvocationResponse)
            return result;

         // Otherwise, create a new WebServerError.
         if(result instanceof String)
         {
            WebServerError ex = new WebServerError((String)result);
            throw ex;
         }
         else
         {
            WebServerError ex = new WebServerError("Error received when calling on web server.  Error returned was " + responseCode);
            throw ex;
         }

      }

      return result;
   }

   private Object checkForLeasePing(HttpURLConnection conn, Object invocation, Map metadata) throws IOException
   {
      InvocationResponse response = null;
      boolean shouldLease = false;
      long leasePeriod = -1;

      if(invocation != null && invocation instanceof InvocationRequest)
      {
         InvocationRequest request = (InvocationRequest)invocation;

         Object payload = request.getParameter();
         // although a bit of a hack, this will determin if first time ping called by client.
         if(payload != null && payload instanceof String && "$PING$".equalsIgnoreCase((String)payload) && request.getReturnPayload() != null)
         {
            try
            {
               // now know is a ping request, so convert to be a HEAD method call
               conn.setDoOutput(false);
               conn.setDoInput(true);
               conn.setRequestMethod("HEAD");
               // set the remoting version
               conn.setRequestProperty(HTTPMetadataConstants.REMOTING_VERSION_HEADER, new Integer(getVersion()).toString());
               // set the user agent
               conn.setRequestProperty(HTTPMetadataConstants.REMOTING_USER_AGENT, "JBossRemoting - " + Version.VERSION);
               conn.setRequestProperty(HTTPMetadataConstants.REMOTING_LEASE_QUERY, "true");
               conn.setRequestProperty("sessionId", request.getSessionId());
               connect(conn);

               //InputStream is = (conn.getResponseCode() < 400) ? conn.getInputStream() : conn.getErrorStream();
               Map headers = conn.getHeaderFields();

               if(headers != null)
               {
                  Object leasingEnabled = headers.get("LEASING_ENABLED");
                  if(leasingEnabled != null && leasingEnabled instanceof List)
                  {
                     shouldLease = new Boolean((String)((List)leasingEnabled).get(0)).booleanValue();
                  }
                  Object leasingPeriod = headers.get("LEASE_PERIOD");
                  if(leasingPeriod != null && leasingPeriod instanceof List)
                  {
                     leasePeriod = new Long((String)((List)leasingPeriod).get(0)).longValue();
                  }
               }
            }
            catch (IOException e)
            {
               log.error("Error checking server for lease information.", e);
            }

            Map p = new HashMap();
            p.put("clientLeasePeriod", new Long(leasePeriod));
            InvocationResponse innterResponse = new InvocationResponse(null, new Boolean(shouldLease), false, p);
            response = new InvocationResponse(null, innterResponse, false, null);

         }
      }

      return response;
   }

   private Object readResponse(Map metadata, Map headers, UnMarshaller unmarshaller, InputStream is)
         throws  ClassNotFoundException, IOException
   {
      Object result = null;
      String encoding = null;
      Object ceObj = headers.get("Content-Encoding");
      if (ceObj != null)
      {
         if (ceObj instanceof List)
         {
            encoding = (String) ((List) ceObj).get(0);
         }
      }
      if (encoding != null && encoding.indexOf("gzip") >= 0)
      {
         unmarshaller = new CompressingUnMarshaller(MarshalFactory.getUnMarshaller(SerializableUnMarshaller.DATATYPE));
      }

      Map map = metadata == null ? new HashMap(headers) : metadata;
      
      // UnMarshaller may not be an HTTPUnMarshaller, in which case it
      // can ignore this parameter.
      if (map.get(HTTPUnMarshaller.PRESERVE_LINES) == null)
      {
         Object o = configuration.get(HTTPUnMarshaller.PRESERVE_LINES);
         if (o != null)
            map.put(HTTPUnMarshaller.PRESERVE_LINES, o);
      }
      
      map.put(HTTPMetadataConstants.USE_REMOTING_CONTENT_TYPE, Boolean.toString(useRemotingContentType));
      
      try
      {
         if (unmarshaller instanceof VersionedUnMarshaller)
            result = ((VersionedUnMarshaller)unmarshaller).read(is, map, getVersion());
         else
            result = unmarshaller.read(is, map);
      }
      catch (ClassNotFoundException e)
      {
         throw e;
      }
      catch (IOException e)
      {
         log.trace(this + " unable to read response", e);
         if (-1 == is.read())
         {
            throw new EOFException();
         }
         throw e;
      }

      return result;
   }

   private void setChunked(Map metadata, final HttpURLConnection conn)
   {
      String chunkedValue = (String) metadata.get("chunkedLength");
      if (chunkedValue != null && chunkedValue.length() > 0)
      {
         try
         {
            int chunkedLength = Integer.parseInt(chunkedValue);

            /**
             * Since HTTPURLConnection in jdk 1.4 does NOT have a setChunkedStreamingMode() method and
             * the one in jdk 1.5 does, will have to use reflection to see if it exists before trying to set it.
             */
            try
            {
               Class cl = conn.getClass();
               Class[] paramTypes = new Class[] {int.class};
               Method setChunkedLengthMethod = getMethod(cl, "setChunkedStreamingMode", paramTypes);
               setChunkedLengthMethod.invoke(conn, new Object[]{new Integer(chunkedLength)});
            }
            catch (NoSuchMethodException e)
            {
               log.warn("Could not set chunked length (" + chunkedLength + ") on http client transport as method not available with JDK 1.4 (only JDK 1.5 or higher)");
            }
            catch (IllegalAccessException e)
            {
               log.error("Error setting http client connection chunked length.");
               log.debug(e);
            }
            catch (InvocationTargetException e)
            {
               log.error("Error setting http client connection chunked length.");
               log.debug(e);
            }
            catch (Exception e)
            {
               // Unexpected.
               log.error("Unexpected error setting http client connection chunked length.");
               log.debug(e);
            }
         }
         catch (NumberFormatException e)
         {
            log.error("Could not set chunked length for http client connection because value (" + chunkedValue + ") is not a number.");
         }


      }
   }


   private int getSimulatedTimeout(Map configuration, Map metadata, final HttpURLConnection conn)
   {
      int timeout = -1;
      String connectionTimeout = (String) configuration.get("timeout");
      String invocationTimeout = (String) metadata.get("timeout");
      
      if (invocationTimeout != null && invocationTimeout.length() > 0)
      {
         try
         {
            timeout = Integer.parseInt(invocationTimeout);
         }
         catch (NumberFormatException e)
         {
            log.error("Could not set timeout for current invocation because value (" + invocationTimeout + ") is not a number.");
         }
      }
      
      if (timeout < 0 && connectionTimeout != null && connectionTimeout.length() > 0)
      {
         try
         {
            timeout = Integer.parseInt(connectionTimeout);
         }
         catch (NumberFormatException e)
         {
            log.error("Could not set timeout for http client connection because value (" + connectionTimeout + ") is not a number.");
         }
      }
      
      if (timeout < 0)
         timeout = 0;

      /**
       * Since URLConnection in jdk 1.4 does NOT have a setConnectTimeout() method and
       * the one in jdk 1.5 does, will have to use reflection to see if it exists before
       * trying to set it.
       */
      try
      {
         Class cl = conn.getClass();
         Class[] paramTypes = new Class[] {int.class};
         Method setTimeoutMethod = getMethod(cl, "setConnectTimeout", paramTypes);
         setTimeoutMethod.invoke(conn, new Object[]{new Integer(timeout)});
         setTimeoutMethod = getMethod(cl, "setReadTimeout", paramTypes);
         setTimeoutMethod.invoke(conn, new Object[]{new Integer(timeout)});
         return -1;
      }
      catch (NoSuchMethodException e)
      {
         log.debug("Using older JDK (prior to 1.5): will simulate timeout");
      }
      catch (IllegalAccessException e)
      {
         log.error("Error setting http client connection timeout.");
         log.debug(e);
      }
      catch (InvocationTargetException e)
      {
         log.error("Error setting http client connection timeout.");
         log.debug(e);
      }
      catch (Exception e)
      {
         // Unexpected.
         log.error("Unexpected error setting http client connection timeout.");
         log.debug(e);
      }

      return timeout;
   }

   protected String validateURL(String url)
   {
      String validatedUrl = url;

      if (validatedUrl.startsWith("servlet"))
      {
         // servlet:// is a valid protocol, but only in the remoting world, so need to convert to http
         validatedUrl = "http" + validatedUrl.substring("servlet".length());
      }
      return validatedUrl;
   }
   
   protected Home getUsableAddress()
   {
      InvokerLocator savedLocator = locator;
      String protocol = savedLocator.getProtocol();
      String path = savedLocator.getPath();
      Map params = savedLocator.getParameters();
      List homes = getConnectHomes();
      
      Iterator it = homes.iterator();
      while (it.hasNext())
      {
         Home home = null;
         try
         {
            home = (Home) it.next();
            locator = new InvokerLocator(protocol, home.host, home.port, path, params);
            invoke(new InvocationRequest(null, null, ServerInvoker.ECHO, null, null, null));
            if (log.isTraceEnabled()) log.trace(this + " able to contact server at: " + home);
            return home;
         }
         catch (Throwable e)
         {
            log.debug(this + " unable to contact server at: " + home);
         }
         finally
         {
            locator = savedLocator;
         }
      }
   
      return null;
   }

   protected HttpURLConnection createURLConnection(String url, Map metadata) throws IOException
   {
      URL externalURL = null;
      HttpURLConnection httpURLConn = null;

      // need to find out if need to use a proxy or not
      String proxyHost = null;
      String proxyportString = null;
      int proxyPort = 80;

      if (metadata != null)
      {
         // first check the metadata as will have precedence
         proxyHost = (String) metadata.get("http.proxyHost");
         proxyportString = (String) metadata.get("http.proxyPort");
         if (proxyportString != null && proxyportString.length() > 0)
         {
            try
            {
               proxyPort = Integer.parseInt(proxyportString);
            }
            catch (NumberFormatException e)
            {
               log.warn("Error converting proxy port specified (" + proxyportString + ") to a number.");
            }
         }
      }

      // now determin if going to use proxy or not
      if (proxyHost != null)
      {
         externalURL = new URL(url);

         /**
          * Since URL in jdk 1.4 does NOT have a openConnection(Proxy) method and
          * the one in jdk 1.5 does, will have to use reflection to see if it exists before trying to set it.
          */
         try
         {
            final Class proxyClass = ClassLoaderUtility.loadClass("java.net.Proxy", HTTPClientInvoker.class);
            InetSocketAddress proxyAddress = new InetSocketAddress(proxyHost, proxyPort);
            Class[] decalredClasses = proxyClass.getDeclaredClasses();
            Class proxyTypeClass = null;
            for(int x = 0; x < decalredClasses.length; x++)
            {
               Class declaredClass = decalredClasses[x];
               String className = declaredClass.getName();
               if(className.endsWith("Type"))
               {
                  proxyTypeClass = declaredClass;
                  break;
               }
            }
            Object proxyType = null;
            Field[] fields = proxyTypeClass.getDeclaredFields();
            for(int i = 0; i < fields.length; i++)
            {
               Field field = fields[i];
               String fieldName = field.getName();
               if(fieldName.endsWith("HTTP"))
               {
                  proxyType = field.get(proxyTypeClass);
                  break;
               }
            }
            Constructor proxyConstructor = proxyClass.getConstructor(new Class[] {proxyTypeClass, SocketAddress.class});
            Object proxy = proxyConstructor.newInstance(new Object[] {proxyType, proxyAddress});
            Method openConnection = getMethod(URL.class, "openConnection", new Class[] {proxyClass});
            httpURLConn = (HttpURLConnection)openConnection.invoke(externalURL, new Object[] {proxy});
         }
         catch (Exception e)
         {
            log.error("Can not set proxy for http invocation (proxy host: " + proxyHost + ", proxy port: " + proxyPort + ") " +
                      "as this configuration requires JDK 1.5 or later.  If running JDK 1.4, can use proxy by setting system properties.");
            log.debug(e);
         }

         // since know it is a proxy being used, see if have proxy auth
         String proxyAuth = getProxyAuth(metadata);
         if (proxyAuth != null)
         {
            httpURLConn.setRequestProperty("Proxy-Authorization", proxyAuth);
         }
      }
      else
      {
         externalURL = new URL(url);
         httpURLConn = (HttpURLConnection) externalURL.openConnection();
         
         // Check if proxy is being configured by system properties.
         if (getSystemProperty("http.proxyHost") != null)
         {
            String proxyAuth = getProxyAuth(metadata);
            if (proxyAuth != null)
            {
               httpURLConn.setRequestProperty("Proxy-Authorization", proxyAuth);
            }
         }
      }

      return httpURLConn;
   }

   private String getProxyAuth(Map metadata)
   {
      String authString = null;
      String username = null;
      String password = null;

      if (metadata != null)
      {
         username = (String) metadata.get("http.proxy.username");
      }
      if (username == null || username.length() == 0)
      {
         username = getSystemProperty("http.proxy.username");
      }
      if (metadata != null)
      {
         password = (String) metadata.get("http.proxy.password");
      }
      if (password == null)
      {
         password = getSystemProperty("http.proxy.password");
      }

      if (username != null && password != null)
      {
         StringBuffer buffer = new StringBuffer();
         buffer.append(username);
         buffer.append(":");
         buffer.append(password);

         String encoded = Base64.encodeBytes(buffer.toString().getBytes());

         authString = "Basic " + encoded;

      }

      return authString;
   }

   private String getBasicAuth(Map metadata)
   {
      String authString = null;
      String username = null;
      String password = null;

      if (metadata != null)
      {
         username = (String) metadata.get("http.basic.username");
      }
      if (username == null || username.length() == 0)
      {
         username = getSystemProperty("http.basic.username");
      }
      if (metadata != null)
      {
         password = (String) metadata.get("http.basic.password");
      }
      if (password == null)
      {
         password = getSystemProperty("http.basic.password");
      }

      if (username != null && password != null)
      {
         StringBuffer buffer = new StringBuffer();
         buffer.append(username);
         buffer.append(":");
         buffer.append(password);

         String encoded = Base64.encodeBytes(buffer.toString().getBytes(), Base64.DONT_BREAK_LINES);

         authString = "Basic " + encoded;

      }

      return authString;
   }


   /**
    * subclasses must implement this method to provide a hook to connect to the remote server, if this applies
    * to the specific transport. However, in some transport implementations, this may not make must difference since
    * the connection is not persistent among invocations, such as SOAP.  In these cases, the method should
    * silently return without any processing.
    *
    * @throws org.jboss.remoting.ConnectionFailedException
    *
    */
   protected void handleConnect() throws ConnectionFailedException
   {
      if (InvokerLocator.MULTIHOME.equals(locator.getHost()))
      {
         Home home = getUsableAddress();
         if (home == null)
         {
            throw new ConnectionFailedException(this + " unable to find a usable address for: " + home);
         }
         
         String protocol = locator.getProtocol();
         String path = locator.getPath();
         Map params = locator.getParameters();
         locator = new InvokerLocator(protocol, home.host, home.port, path, params);
         if (log.isDebugEnabled()) log.debug(this + " will use InvokerLocator " + locator);
      }
   }

   /**
    * subclasses must implement this method to provide a hook to disconnect from the remote server, if this applies
    * to the specific transport. However, in some transport implementations, this may not make must difference since
    * the connection is not persistent among invocations, such as SOAP.  In these cases, the method should
    * silently return without any processing.
    */
   protected void handleDisconnect()
   {
      // NO OP as not statefull connection
   }

   /**
    * Each implementation of the remote client invoker should have
    * a default data type that is uses in the case it is not specified
    * in the invoker locator uri.
    *
    * @return
    */
   protected String getDefaultDataType()
   {
      return HTTPMarshaller.DATATYPE;
   }


   /**
    * Sets the thread pool to be used for simulating timeouts with jdk 1.4.
    */
   public void setTimeoutThreadPool(ThreadPool pool)
   {
      this.timeoutThreadPool = pool;
   }
   
   protected void configureParameters()
   {
      Object val = configuration.get(HTTPMetadataConstants.NO_THROW_ON_ERROR);
      if (val != null)
      {
         try
         {
            noThrowOnError = Boolean.valueOf((String)val).booleanValue();
            log.debug(this + " setting noThrowOnError to " + noThrowOnError);
         }
         catch (Exception e)
         {
            log.warn(this + " could not convert " + 
                     HTTPMetadataConstants.NO_THROW_ON_ERROR + " value of " +
                     val + " to a boolean value.");
         }
      }
      
      val = configuration.get(NUMBER_OF_CALL_ATTEMPTS);
      if (val != null)
      {
         try
         {
            numberOfCallAttempts = Integer.valueOf((String)val).intValue();
            log.debug(this + " setting numberOfCallRetries to " + numberOfCallAttempts);
         }
         catch (Exception e)
         {
            log.warn(this + " could not convert " + 
                     NUMBER_OF_CALL_ATTEMPTS + " value of " +
                     val + " to an int value.");
         }
      }
      
      val = configuration.get(UNMARSHAL_NULL_STREAM);
      if (val != null)
      {
         try
         {
            unmarshalNullStream = Boolean.valueOf((String)val).booleanValue();
            log.debug(this + " setting unmarshalNullStream to " + unmarshalNullStream);
         }
         catch (Exception e)
         {
            log.warn(this + " could not convert " + 
                     UNMARSHAL_NULL_STREAM + " value of " +
                     val + " to a boolean value.");
         }
      }

      val = configuration.get(HTTPMetadataConstants.USE_REMOTING_CONTENT_TYPE);
      if (val != null)
      {
         try
         {
            useRemotingContentType = Boolean.valueOf((String)val).booleanValue();
            log.debug(this + " setting useRemotingContent to " + useRemotingContentType);
         }
         catch (Exception e)
         {
            log.warn(this + " could not convert " + 
                     HTTPMetadataConstants.USE_REMOTING_CONTENT_TYPE + " value of " +
                     val + " to a boolean value.");
         }
      }
   }
   
   /**
    * Gets the thread pool being used for simulating timeouts with jdk 1.4. If one has
    * not be specifically set via configuration or call to set it, will always return
    * instance of org.jboss.util.threadpool.BasicThreadPool.
    */
   public ThreadPool getTimeoutThreadPool()
   {
      synchronized (timeoutThreadPoolLock)
      {
         if (timeoutThreadPool == null)
         {
            int maxNumberThreads = MAX_NUM_TIMEOUT_THREADS_DEFAULT;
            int maxTimeoutQueueSize = -1;
            
            BasicThreadPool pool = new BasicThreadPool("HTTP timeout");
            log.debug("created new thread pool: " + pool);
            Object param = configuration.get(MAX_NUM_TIMEOUT_THREADS);
            if (param instanceof String)
            {
               try
               {
                  maxNumberThreads = Integer.parseInt((String) param);
               }
               catch (NumberFormatException  e)
               {
                  log.error("maxNumberThreads parameter has invalid format: " + param);
               }
            }
            else if (param != null)
            {
               log.error("maxNumberThreads parameter must be a string in integer format: " + param);
            }

            param = configuration.get(MAX_TIMEOUT_QUEUE_SIZE);

            if (param instanceof String)
            {
               try
               {
                  maxTimeoutQueueSize = Integer.parseInt((String) param);
               }
               catch (NumberFormatException  e)
               {
                  log.error("maxTimeoutQueueSize parameter has invalid format: " + param);
               }
            }
            else if (param != null)
            {
               log.error("maxTimeoutQueueSize parameter must be a string in integer format: " + param);
            }

            pool.setMaximumPoolSize(maxNumberThreads);

            if (maxTimeoutQueueSize > 0)
            {
               pool.setMaximumQueueSize(maxTimeoutQueueSize);
            }
            pool.setBlockingMode(BlockingMode.RUN);
            timeoutThreadPool = pool;
         }
      }
      return timeoutThreadPool;
   }
   
   
   /**
    * When a WaitingTaskWrapper is run in a BasicThreadPool, the calling thread
    * will block for the designated timeout period.
    */
   static class WaitingTaskWrapper extends RunnableTaskWrapper
   {
      long completeTimeout;
      
      public WaitingTaskWrapper(Runnable runnable, long completeTimeout)
      {
         super(runnable, 0, completeTimeout);
         this.completeTimeout = completeTimeout;
      }
      public int getTaskWaitType()
      {
         return Task.WAIT_FOR_COMPLETE;
      }
      public String toString()
      {
         return "WaitingTaskWrapper[" + completeTimeout + "]";
      }
   }
   
   static private String getSystemProperty(final String name)
   {
      if (SecurityUtility.skipAccessControl())
         return System.getProperty(name);
      
      String value = null;
      try
      {
         value = (String)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return System.getProperty(name);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RuntimeException) e.getCause();
      }
      
      return value;
   }
   
   static private Method getMethod(final Class c, final String name, final Class[] parameterTypes)
   throws NoSuchMethodException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return c.getMethod(name, parameterTypes);
      }

      try
      {
         return (Method) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws NoSuchMethodException
            {
               return c.getMethod(name, parameterTypes);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (NoSuchMethodException) e.getCause();
      }
   }
   
   static private void connect(final HttpURLConnection conn) throws IOException
   {
      if (SecurityUtility.skipAccessControl())
      {
         conn.connect();
         return;
      }

      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IOException
            {
               conn.connect();
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (IOException) e.getCause();
      }
   }
   
   static private OutputStream getOutputStream(final HttpURLConnection conn)
   throws IOException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return conn.getOutputStream();
      }
      
      try
      {
         return (OutputStream)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IOException
            {
               return conn.getOutputStream();
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (IOException) e.getCause();
      }
   }
   
   static private int getResponseCode(final HttpURLConnection conn)
   throws IOException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return conn.getResponseCode();
      }
      
      try
      {
         return ((Integer) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IOException
            {
               return new Integer(conn.getResponseCode());
            }
         })).intValue();
      }
      catch (PrivilegedActionException e)
      {
         throw (IOException) e.getCause();
      }
   }
   
   static private String getResponseMessage(final HttpURLConnection conn)
   throws IOException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return conn.getResponseMessage();
      }
      
      try
      {
         return (String) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IOException
            {
               return conn.getResponseMessage();
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (IOException) e.getCause();
      }
   }
}