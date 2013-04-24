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

package org.jboss.remoting.samples.transporter.complex.server;

import org.jboss.remoting.samples.transporter.complex.ProviderInterface;
import org.jboss.remoting.samples.transporter.complex.ProviderInterfaceImpl;
import org.jboss.remoting.transporter.TransporterServer;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class Server
{
   public static String locatorURI = "socket://localhost:5401/?serializationtype=jboss";
   private TransporterServer server = null;

   public void start() throws Exception
   {
      server = TransporterServer.createTransporterServer(getLocatorURI(), new ProviderInterfaceImpl(),
                                                         ProviderInterface.class.getName(), true);
   }

   protected String getLocatorURI()
   {
      return locatorURI;
   }

   public void stop()
   {
      if(server != null)
      {
         server.stop();
      }
   }

   public static void main(String[] args)
   {
      Server server = new Server();
      try
      {
         server.start();

         Thread.currentThread().sleep(60000);

      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
      finally
      {
         server.stop();
      }
   }
}