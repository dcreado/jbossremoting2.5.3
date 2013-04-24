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

package org.jboss.test.remoting.performance.asynchronous;

import org.jboss.jrunit.decorators.ThreadLocalDecorator;
import org.jboss.test.remoting.performance.synchronous.Payload;
import org.jboss.test.remoting.performance.synchronous.PerformanceClientTest;

import junit.framework.Test;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class PerformanceServerSideClientTest extends PerformanceClientTest
{
   public static Test suite()
   {
      return new ThreadLocalDecorator(PerformanceServerSideClientTest.class, 1);
   }

   protected String getBenchmarkName()
   {
      return "PerformanceServerSideClientTest";
   }

   protected Object makeClientCall(String testInvocation, Payload payloadWrapper) throws Throwable
   {
      return makeOnewayInvocation(testInvocation, payloadWrapper, false);
   }

   protected void verifyResults(int x, Object resp)
   {
      //NO OP since no return
   }

   public static void main(String[] args)
   {
      PerformanceServerSideClientTest test = new PerformanceServerSideClientTest();
      try
      {
         test.setUp();
         test.testClientCalls();
      }
      catch(Throwable throwable)
      {
         throwable.printStackTrace();
      }
      finally
      {
         try
         {
            test.tearDown();
         }
         catch(Exception e)
         {
            e.printStackTrace();
         }
      }
   }
}