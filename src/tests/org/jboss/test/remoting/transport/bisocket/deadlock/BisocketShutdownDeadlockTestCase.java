/***************************************
 *                                     *
 *  JBoss: The OpenSource J2EE WebOS   *
 *                                     *
 *  Distributable under LGPL license.  *
 *  See terms of license at gnu.org.   *
 *                                     *
 ***************************************/
package org.jboss.test.remoting.transport.bisocket.deadlock;

import org.jboss.test.remoting.transport.socket.deadlock.ShutdownDeadlockTestCase;

/**
 * This test case is for JBREM-576.
 * Test trys to catch deadlock in shutdown where socket client
 * invoker being use my multiple Clients.  Need one client making
 * invocations (which will cause sync on pool) and another to
 * shutdown, which will cause disconnect on socket client invoker,
 * thus causing it to sync on pool for clearing out the pool.
 * Since this is an issue of multithreading, is certainly possible
 * this test will pass even though the deadlock issue still exists.
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 * @author <a href="mailto:ron.sigal@jboss.org">Ron Sigal</a>
 */
public class BisocketShutdownDeadlockTestCase extends ShutdownDeadlockTestCase
{
   protected String getTransport()
   {
      return "bisocket";
   }
   
   protected int getPort()
   {
      return super.getPort() + 11;
   }
}