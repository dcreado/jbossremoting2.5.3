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
package org.jboss.test.remoting.transport.socket.shutdown;

import java.net.InetAddress;

import org.jboss.remoting.transport.PortUtil;
import org.jboss.test.remoting.shutdown.OpenClient;
import org.jboss.test.remoting.shutdown.ShutdownTestParent;
import org.jboss.test.remoting.shutdown.ShutdownTestServer;

/** 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 3500 $
 * <p>
 * Copyright Jan 20, 2007
 * </p>
 */
public class SocketShutdownTestCase extends ShutdownTestParent
{
   public void testWithServerThreadsInAcknowledge() throws Throwable
   {
      log.info("entering " + getName());
      
      port = PortUtil.findFreePort(InetAddress.getLocalHost().getHostName());
      String command = "java -cp \"" +  System.getProperty("java.class.path") + "\" ";
      command += getJVMArguments() + " -Dport=" + port + " ";
      String serverCommand = command + ShutdownTestServer.class.getName() + " " + getTransport();
      serverCommand += " socket.check_connection=true&";
      String clientCommand = command + OpenClient.class.getName() + " " + getTransport();
      clientCommand += " socket.check_connection=true&";
      Executor serverExecutor = new Executor(serverCommand, true);
      log.info("starting server");
      serverExecutor.start();
      log.info("waiting on server");
      serverExecutor.waitUntilReady();
      log.info("server is ready");
      Executor clientExecutor = new Executor(clientCommand, false);
      log.info("starting client");
      clientExecutor.start();
      log.info("waiting on client");
      clientExecutor.waitUntilReady();
      log.info("client is ready");
      clientExecutor.start();
      Thread.sleep(40000);
      log.info("testing client");
      assertFalse(clientSuccessful);
      log.info("testing server");
      assertTrue(serverSuccessful);
      clientExecutor.destroy();
      log.info(getName() + " PASSES");
   }
   
   
   protected String getTransport()
   {
      return "socket";
   }

   protected String getJVMArguments()
   {
      String args = "";
      String log4jFilePath = getClass().getResource("../../../shutdown/log4j.xml").getFile();
      System.out.println("log4jFilePath: " + log4jFilePath);
      args += "-Dlog4j.configuration=file:" + log4jFilePath;
      return args;
   }
}
