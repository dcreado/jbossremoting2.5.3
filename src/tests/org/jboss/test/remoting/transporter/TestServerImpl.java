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

package org.jboss.test.remoting.transporter;

import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.remoting.transporter.TransporterServer;

import java.io.IOException;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class TestServerImpl extends ServerTestCase implements TestServer
{
   private String locatorURI = "socket://localhost:5400";
   private TransporterServer server = null;

   public String processTestMessage(String msg)
   {
      return msg + " - has been processed";
   }

   public int processInt(int msg)
   {
      System.out.println("processing int " + msg);
      return 456;
   }

   public void throwException() throws IOException
   {
      throw new IOException("This is an expected exception thrown by impl on purpose.");
   }

   public void setUp() throws Exception
   {
      server = TransporterServer.createTransporterServer(locatorURI, new TestServerImpl(), TestServer.class.getName());
   }

   public void tearDown()
   {
      if(server != null)
      {
         server.stop();
      }
   }

   /**
    * Just here so can run from a few line within a main
    *
    * @param args
    */
   public static void main(String[] args)
   {
      String locatorURI = "socket://localhost:5400";

      try
      {
         TransporterServer server = TransporterServer.createTransporterServer(locatorURI, new TestServerImpl(), TestServer.class.getName());

         Thread.currentThread().sleep(60000);

         server.stop();
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }

   }
}