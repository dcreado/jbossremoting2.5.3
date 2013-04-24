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
package org.jboss.test.remoting.callback.asynch;

import java.util.Map;

import org.jboss.remoting.transport.http.ssl.HTTPSClientInvoker;

/** 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2110 $
 * <p>
 * Copyright Feb 3, 2007
 * </p>
 */
public class HTTPSAsynchCallbackTestServer extends SSLAsynchCallbackTestServerRoot
{
   public static void main(String[] args)
   {
      AsynchCallbackTestServerRoot testCase = new HTTPSAsynchCallbackTestServer();
      try
      {
         testCase.setUp();
         Thread.sleep(60000);
         testCase.tearDown();
      }
      catch(Throwable e)
      {
         e.printStackTrace();
      }
   }
   
   protected String getTransport()
   {
      return "https";
   }
   
   protected void addTransportSpecificConfig(Map config)
   {
      super.addTransportSpecificConfig(config);
      config.put(HTTPSClientInvoker.IGNORE_HTTPS_HOST, "true");
   }
}
