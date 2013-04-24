/*
* JBoss, a division of Red Hat
* Copyright 2006, Red Hat Middleware, LLC, and individual contributors as indicated
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
package org.jboss.test.remoting.lease.servlet;

import org.jboss.test.remoting.lease.LeaseTestClient;
import org.jboss.remoting.InvokerLocator;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class ServletLeaseTestClient extends LeaseTestClient
{
   // Default locator values
   private static String transport = "servlet";
   private static String host = "localhost";
   private static int port = 8080;

   private String locatorURI = transport + "://" + host + ":" + port + "/servlet-invoker/ServerInvokerServlet/?" + InvokerLocator.CLIENT_LEASE + "=" + "true";

   protected String getLocatorUri()
   {
      return locatorURI;
   }

}