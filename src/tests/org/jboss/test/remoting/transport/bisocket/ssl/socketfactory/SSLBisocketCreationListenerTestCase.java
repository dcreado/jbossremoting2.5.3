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
package org.jboss.test.remoting.transport.bisocket.ssl.socketfactory;

import java.util.Map;

import org.jboss.remoting.transport.bisocket.Bisocket;
import org.jboss.test.remoting.socketfactory.SSLCreationListenerTestRoot;
import org.jboss.test.remoting.socketfactory.TestListener;

/** 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1887 $
 * <p>
 * Copyright Jan 11, 2007
 * </p>
 */
public class SSLBisocketCreationListenerTestCase extends SSLCreationListenerTestRoot
{  
   protected String getTransport()
   {
      return "sslbisocket";
   }
   
   protected void addExtraClientConfig(Map config)
   {
      config.put(Bisocket.IS_CALLBACK_SERVER, "true");
   }
   
   protected boolean checkListenersVisited(TestListener listener1, TestListener listener2,
                                  TestListener listener3, TestListener listener4)
   {
      // Need to treat sslbisocket transport as a special case because sockets are
      // created in a special way.
      return  listener1.visited() &&
              listener2.visited() &&
              listener3.visited() &&
              listener4.visited() &&
             !listener1.isClient() &&
             !listener2.isClient() &&
              listener3.isClient() &&
              listener4.isClient();
   }
}
