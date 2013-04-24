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
package org.jboss.test.remoting.transport.bisocket.shutdown;

import org.jboss.test.remoting.transport.socket.shutdown.SocketShutdownTestCase;

/** 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2947 $
 * <p>
 * Copyright Jan 20, 2007
 * </p>
 */
public class BisocketShutdownTestCase extends SocketShutdownTestCase
{
   protected String getTransport()
   {
      return "bisocket";
   }
   
   protected String getJVMArguments()
   {
      String args = "";
      String log4jFilePath = getClass().getResource("../../../shutdown/log4j.xml").getFile();
      System.out.println("log4jFilePath: " + log4jFilePath);
      args += "-Dlog4j.configuration=file:" + log4jFilePath;
      return args;
   }
   
   protected String getHangingClientClassName()
   {
      log.info("returning HangingClass: " + BisocketHangingClient.class.getName());
      return BisocketHangingClient.class.getName();
   }
   
   protected String getClosingClientClassName()
   {
      log.info("returning ClosingClass: " + BisocketClosingClient.class.getName());
      return BisocketClosingClient.class.getName();
   }
}
