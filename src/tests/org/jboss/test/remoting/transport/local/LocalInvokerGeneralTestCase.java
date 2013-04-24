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

package org.jboss.test.remoting.transport.local;

import org.jboss.logging.Logger;
import org.jboss.remoting.transport.local.LocalClientInvoker;
import org.jboss.test.remoting.transport.InvokerClientTest;
import org.jboss.test.remoting.transport.socket.SocketInvokerServerTest;


/**
 * A LITestCase.

 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * @version $Revision: 576 $
 * <p>
 * Copyright (c) 2005
 * </p>
 */

public class LocalInvokerGeneralTestCase extends InvokerClientTest
{
   private static final Logger log = Logger.getLogger(LocalInvokerGeneralTestCase.class);
   private SocketInvokerServerTest server;
   
   public void setUp()
   {
      server = new SocketInvokerServerTest();
      
      try
      {
         server.setUp();
      }
      catch(Exception e)
      {
         log.error(e);
         e.printStackTrace();
      }
      
      try
      {
         super.setUp();
      }
      catch(Exception e)
      {
         log.error(e);
         e.printStackTrace();
      }
   }
   
   
   public void tearDown()
   {
      try
      {
         server.tearDown();
         super.tearDown();
         Thread.sleep(2000);
      }
      catch(Exception e)
      {
         log.error(e);
         e.printStackTrace();
      }
   }
   
   
   public String getTransport()
   {
      return "socket";
   }

   
   public void testLocalPushCallback() throws Throwable
   {
      assertTrue(getClient().getInvoker().getClass().equals(LocalClientInvoker.class));
      super.testLocalPushCallback();
   }
   
   
   public void testRemotePushCallback() throws Throwable
   {
      assertTrue(getClient().getInvoker().getClass().equals(LocalClientInvoker.class));
      super.testRemotePushCallback();
   }
   
   
   public void testPullCallback() throws Throwable
   {
      assertTrue(getClient().getInvoker().getClass().equals(LocalClientInvoker.class));
      super.testPullCallback();
   }
   
   
   public void testArrayReturn() throws Throwable
   {
      assertTrue(getClient().getInvoker().getClass().equals(LocalClientInvoker.class));
      super.testArrayReturn();
   }
   
   
   public void testThrownException() throws Throwable
   {
      assertTrue(getClient().getInvoker().getClass().equals(LocalClientInvoker.class));
      super.testThrownException();
   }
}

