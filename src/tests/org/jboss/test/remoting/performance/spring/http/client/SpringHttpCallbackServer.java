/***************************************
 *                                     *
 *  JBoss: The OpenSource J2EE WebOS   *
 *                                     *
 *  Distributable under LGPL license.  *
 *  See terms of license at gnu.org.   *
 *                                     *
 ***************************************/
package org.jboss.test.remoting.performance.spring.http.client;

import EDU.oswego.cs.dl.util.concurrent.Latch;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.test.remoting.performance.synchronous.PerformanceCallbackKeeper;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public interface SpringHttpCallbackServer extends PerformanceCallbackKeeper, InvokerCallbackHandler
{
    public void finishedProcessing(Object obj);

   void setClientSessionId(String clientSessionId);

   void setServerDoneLock(Latch serverDoneLock);
}
