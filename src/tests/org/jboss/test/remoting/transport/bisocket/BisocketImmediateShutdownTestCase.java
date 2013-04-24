package org.jboss.test.remoting.transport.bisocket;

import org.jboss.test.remoting.transport.socket.shutdown.SocketImmediateShutdownTestCase;

/**
 * Unit test for JBREM-1123.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Rev$
 * <p>
 * Copyright Apr 19, 2009
 * </p>
 */
public class BisocketImmediateShutdownTestCase extends SocketImmediateShutdownTestCase
{
   protected String getTransport()
   {
      return "bisocket";
   }
}
