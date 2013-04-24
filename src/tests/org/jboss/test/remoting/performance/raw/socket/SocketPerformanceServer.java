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

package org.jboss.test.remoting.performance.raw.socket;

import org.jboss.test.remoting.performance.synchronous.PerformanceServerTest;
import org.jboss.test.remoting.performance.synchronous.PerformanceTestCase;

import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class SocketPerformanceServer extends PerformanceServerTest
{
   private SocketServer server = null;
   private int numOfClients;

   public void init(Map metatdata) throws Exception
   {
      server = new SocketServer(host, numOfClients);
      server.setUp();
   }

   public void setUp() throws Exception
   {
      String numOfClientsString = System.getProperty(PerformanceTestCase.NUMBER_OF_CLIENTS);
      if(numOfClientsString != null && numOfClientsString.length() > 0)
      {
         try
         {
            numOfClients = Integer.parseInt(numOfClientsString);
         }
         catch(NumberFormatException e)
         {
            e.printStackTrace();
         }
      } 
      super.setUp();
   }
   
   public void tearDown() throws Exception
   {
      super.tearDown();
      if(server != null)
      {
         server.tearDown();
      }
   }

   public static void main(String[] args)
   {
      PerformanceServerTest test = new SocketPerformanceServer();
      try
      {
         test.setUp();
         Thread.currentThread().sleep(300000);
         test.tearDown();
         //System.exit(0);
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }

}