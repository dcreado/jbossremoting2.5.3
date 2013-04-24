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

/**
 * There is some metadata that can be expected to be found in the
 * metadata passed to the server invoker handlers for http transports.
 * This class contains the constants for the keys to these values.
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class HTTPMetadataConstants
{
   public static final String METHODTYPE = "MethodType";
   public static final String PATH = "Path";
   public static final String QUERY = "Query";
   public static final String HTTPVERSION = "HttpVersion";
   public static final String RESPONSE_CODE = "ResponseCode";
   public static final String CONTENTTYPE = "Content-Type";
   public static final String RESPONSE_CODE_MESSAGE = "ResponseCodeMessage";
   public static final String REMOTING_VERSION_HEADER = "JBoss-Remoting-Version";
   public static final String REMOTING_USER_AGENT = "User-Agent";
   public static final String REMOTING_LEASE_QUERY = "JBoss-Remoting-Lease-Query";
   // for some reason, if using servlet invoker, the header name is all lower case (at least w/ JBoss/Tomcat)
   public static final String REMOTING_LEASE_QUERY_LOWER_CASE = "jboss-remoting-lease-query";

   /**
    * Key used for returning the value of java.net.URLConnection.getHeaderFields()
    * in the metadata map passed to org.jboss.remoting.Client.invoke().
    */
   public static final String RESPONSE_HEADERS = "ResponseHeaders";
   
   /**
    * Configuration key for indicating if http client invoker should
    * throw exception on error from server or just return the error
    * as the response.
    */
   public static final String NO_THROW_ON_ERROR = "NoThrowOnError";
   
   /**
    * Configuration key for indicating that servlet invoker should return actual exception 
    * thrown by invocation handler.
    */
   public static final String DONT_RETURN_EXCEPTION = "dont-return-exception";

   /** Used to distinguish special case of payload of type String. */
   public static final String REMOTING_CONTENT_TYPE = "remotingContentType";
   public static final String REMOTING_CONTENT_TYPE_LC = "remotingcontenttype";
   
   public static final String REMOTING_CONTENT_TYPE_STRING = "remotingContentTypeString";
   
   public static final String REMOTING_CONTENT_TYPE_NON_STRING = "remotingContentTypeNonString";
   
   public static final String USE_REMOTING_CONTENT_TYPE = "useRemotingContentType";
}