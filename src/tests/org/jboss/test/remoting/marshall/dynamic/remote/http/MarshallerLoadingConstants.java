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

package org.jboss.test.remoting.marshall.dynamic.remote.http;

import org.jboss.remoting.InvokerLocator;

/**
 * @author <a href="mailto:telrod@jboss.com">Tom Elrod</a>
 */
public interface MarshallerLoadingConstants
{
   // Default locator values
   static String transport = "http";
   static String host = "localhost";
   static int port = 5402;
   static int loaderPort = 5403;

   static String dataType = "test";
   static String locatorURI = transport + "://" + host + ":" + port + "/?" +
                              InvokerLocator.DATATYPE + "=" + dataType + "&" +
                              InvokerLocator.LOADER_PORT + "=" + loaderPort + "&" +
                              InvokerLocator.MARSHALLER + "=" + "org.jboss.test.remoting.marshall.dynamic.remote.http.TestMarshaller" + "&" +
                              InvokerLocator.UNMARSHALLER + "=" + "org.jboss.test.remoting.marshall.dynamic.remote.http.TestUnMarshaller";
   ;
}
