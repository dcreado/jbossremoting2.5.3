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

package org.jboss.test.remoting.transport.bisocket;

import org.apache.log4j.Level;
import org.jboss.test.remoting.transport.InvokerServerTest;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class BisocketInvokerServerTest extends InvokerServerTest
{
   public String getTransport()
   {
      return "bisocket";
   }

   public static void main(String[] args)
   {
      org.apache.log4j.BasicConfigurator.configure();
      org.apache.log4j.Category.getRoot().setLevel(Level.INFO);
      org.apache.log4j.Category.getInstance("org.jboss.remoting.transport.socket").setLevel(Level.DEBUG);
      org.apache.log4j.Category.getInstance("org.jboss.test.remoting").setLevel(Level.DEBUG);
//      org.apache.log4j.Category.getInstance("org.jboss.remoting.marshall.dynamic.local").setLevel(Level.DEBUG);
//      org.apache.log4j.Category.getInstance("org.jboss.dtf").setLevel(Level.DEBUG);
      org.apache.log4j.Category.getInstance("org.jgroups").setLevel(Level.FATAL);

      InvokerServerTest server = new BisocketInvokerServerTest();
      try
      {
         server.setUp();
         Thread.sleep(300000);
         server.tearDown();
         System.out.println("Have torn down test.");
         Thread.sleep(30000);
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }

}