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
package org.jboss.test.remoting.transport.bisocket.socketpool;

import org.apache.log4j.Level;
import org.jboss.test.remoting.transport.InvokerTestDriver;

/**
 *  This unit test is introduced in response to JIRA issue JBREM-625.
 *  It verifies that when a socket is discarded, the count of sockets
 *  in use is decremented.
 *  
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 *  
 * @version $Revision: 1732 $
 * <p>
 * Copyright Dec 15, 2006
 * </p>
 */
public class BisocketPoolTestCase extends InvokerTestDriver
{
   public void declareTestClasses()
   {  
      addTestClasses(BisocketPoolTestClient.class.getName(),
                     1,
                     BisocketPoolTestServer.class.getName());
   }

   protected Level getTestLogLevel()
   {
      return Level.DEBUG;
   }

   /**
    * How long to wait for test results to be returned from the client(s).  If goes longer than the
    * specified limit, will throw an exception and kill the running test cases.  Default value is
    * RESULTS_TIMEOUT.
    *
    * @return
    */
   protected long getResultsTimeout()
   {
      return 600000;
   }

   /**
    * How long for the server test case to wait for tear down message.  If exceeds timeout,
    * will throw exception.  The default value is TEARDOWN_TIMEOUT.
    *
    * @return
    */
   protected long getTearDownTimeout()
   {
      return 600000;
   }

   /**
    * How long to allow each of the test cases to run their tests.  If exceeds this timeout
    * will throw exception and kill tests.  The default value is RUN_TEST_TIMEOUT.
    *
    * @return
    */
   protected long getRunTestTimeout()
   {
      return 600000;
   }

}
