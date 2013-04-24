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

package org.jboss.test.remoting.transport.servlet.ssl;

import org.jboss.test.remoting.transport.web.WebInvokerTestClient;

/**
 * This test is identical to ServletInvokerTestClient except that it, in the case
 * when an exception is thrown on the server, ServletServerInvoker is instructed
 * to return the actual exception.
 * 
 * See JBREM-813.
 * 
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 */
public class SSLServletInvokerExceptionTestClient extends WebInvokerTestClient
{
   public static final Integer ERROR_RESPONSE_CODE = new Integer(500);

   public String getLocatorURI()
   {
      // since doing basic (using default ssl server socket factory)
      // need to set the system properties to the truststore
      String trustStoreFilePath = this.getClass().getResource("truststore").getFile();
      System.setProperty("javax.net.ssl.trustStore", trustStoreFilePath);

      return "sslservlet://localhost:8443/servlet-invoker/ServerInvokerServlet";
      //return "http://localhost:8080/servlet-invoker/ServerInvokerServlet";
   }

   public static void main(String[] args)
   {
      SSLServletInvokerExceptionTestClient client = new SSLServletInvokerExceptionTestClient();
      try
      {
         client.testPostInvocation();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

}