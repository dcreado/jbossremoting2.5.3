package org.jboss.test.remoting.transport.bisocket.socketfactory;

import org.jboss.test.remoting.socketfactory.SocketFactoryClassNameTestRoot;


/**
 * 
 * Unit test for JBREM-1014.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Jul 18, 2008
 * </p>
 */
public class SocketFactoryClassNameTestCase extends SocketFactoryClassNameTestRoot
{
   protected String getTransport()
   {
      return "bisocket";
   }
}