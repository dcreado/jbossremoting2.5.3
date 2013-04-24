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
package org.jboss.test.remoting.performance.profiler;

import org.jboss.test.remoting.performance.synchronous.PerformanceServerTest;
import org.jboss.test.remoting.performance.synchronous.PerformanceClientTest;
import org.jboss.test.remoting.performance.synchronous.PerformanceTestCase;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class ProfileTest
{
//   private String numOfCalls = "100";
   private String numOfCalls = "10000";
//   private String numOfCalls = "1000000";

   public void profilerTest() throws Throwable
   {
      PerformanceServerTest server = new PerformanceServerTest();
      server.setUp();
      System.out.println("Server setup");

      System.setProperty(PerformanceTestCase.NUMBER_OF_CALLS, numOfCalls);
      System.setProperty(PerformanceTestCase.REMOTING_METADATA, "foo=bar");

      PerformanceClientTest client = new PerformanceClientTest();
      client.setUp();
      System.out.println("Client setup");
      client.testClientCalls();
      System.out.println("Done with testing client calls");


      client.tearDown();
      server.tearDown();
   }

   public static void main(String[] args)
   {
      ProfileTest test = new ProfileTest();
      try
      {
         test.profilerTest();
      }
      catch(Throwable throwable)
      {
         throwable.printStackTrace();
      }
   }
}