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
package org.jboss.test.remoting.transport.http.compression;

import org.jboss.test.remoting.transport.http.HTTPInvokerTestServer;
import org.apache.log4j.Level;

import java.util.Map;
import java.util.HashMap;

/**
 * This test turns on compression for tomcat.  When using compression=on for config,
 * tomcat will only compress responses with size > 1024 (and the request header having
 * Accept-Encoding).  Therefore, only some of the responses in this test will be compressed
 * (actually only the fourth response from org.jboss.test.remoting.transport.web.WebInvokerTestClient
 * as it sends a ComplexObject which triggers the sending of WebInvocationHandler.LARGE_OBJECT_RESPONSE_VALUE).
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class CompressedHTTPInvokerTestServer extends HTTPInvokerTestServer
{
   public void init(Map metatdata) throws Exception
   {
      if(metatdata == null)
      {
         metatdata = new HashMap();
      }
      // adding config to force tomcat compression
      metatdata.put("compression", "on");
      metatdata.put("compressableMimeType", "text/html,text/xml," +
                                            "text/plain,application/x-www-form-urlencoded,application/x-java-serialized-object," +
                                            "application/octet-stream,application/soap+xml");

//      metatdata.put("serialization", "jboss");

      super.init(metatdata);
   }

   public static void main(String[] args)
   {
      org.apache.log4j.BasicConfigurator.configure();
      org.apache.log4j.Category.getRoot().setLevel(Level.INFO);
      org.apache.log4j.Category.getInstance("org.jgroups").setLevel(Level.INFO);
      org.apache.log4j.Category.getInstance("org.jboss.remoting").setLevel(Level.DEBUG);
      org.apache.log4j.Category.getInstance("test").setLevel(Level.DEBUG);

      CompressedHTTPInvokerTestServer server = new CompressedHTTPInvokerTestServer();
      try
      {
         server.setUp();
         Thread.currentThread().sleep(300000);
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
      finally
      {
         try
         {
            server.tearDown();
         }
         catch(Exception e)
         {
            e.printStackTrace();
         }
      }
   }

}