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
package org.jboss.test.remoting.shutdown;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.remoting.transport.PortUtil;

/** 
 * This unit test is meant to guard against the possibility of accidentally
 * creating non-daemon threads that prevent the Remoting subsystem from 
 * terminating.
 * 
 * See JIRA issue JBREM-674 "add test case for client exiting correctly".
 * (http://jira.jboss.com/jira/browse/JBREM-674)
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 3011 $
 * <p>
 * Copyright Jan 19, 2007
 * </p>
 */
public abstract class ShutdownTestParent extends TestCase
{
   protected static Logger log = Logger.getLogger(ShutdownTestParent.class);
   protected static boolean firstTime = true;
   
   protected boolean serverSuccessful;
   protected boolean clientSuccessful;
   protected int port;
   
   
   public void setUp()
   {
      if (firstTime)
      {
         firstTime = false;
         Logger.getLogger("org.jboss.remoting").setLevel(Level.INFO);
         Logger.getLogger("org.jboss.test.remoting").setLevel(Level.INFO);
         String pattern = "[%d{ABSOLUTE}] [%t] %5p (%F:%L) - %m%n";
         PatternLayout layout = new PatternLayout(pattern);
         ConsoleAppender consoleAppender = new ConsoleAppender(layout);
         Logger.getRootLogger().addAppender(consoleAppender);  
      }
      
      serverSuccessful = false;
      clientSuccessful = false;
   }
   
   /**
    * This test case verifies that the test really works.  In particular,
    * a non-daemon thread is created in the client JVM that prevents it
    * from shutting down in the expected time frame.
    */
   public void testHangingClient() throws Throwable
   {
      log.info("entering " + getName());
      
      port = PortUtil.findFreePort(InetAddress.getLocalHost().getHostName());
      String command = "java -cp \"" +  System.getProperty("java.class.path") + "\" ";
      command += getJVMArguments() + " -Dport=" + port + " ";
      String serverCommand = command + ShutdownTestServer.class.getName() + " " + getTransport();
      serverCommand += " " + getServerArgs();
      String clientCommand = command + getHangingClientClassName() + " " + getTransport();
      clientCommand += " " + getClientArgs();
 
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
      Thread.sleep(15000);
      log.info("testing client");
      assertFalse(clientSuccessful);
      Thread.sleep(15000);
      log.info("testing server");
      assertTrue(serverSuccessful);
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * This test verifies that a JVM with a client, and another JVM with a server,
    * both terminate.
    */
   public void testClosingClient() throws Throwable
   {
      log.info("entering " + getName());
      
      port = PortUtil.findFreePort(InetAddress.getLocalHost().getHostName());
      String command = "java -cp \"" +  System.getProperty("java.class.path") + "\" ";
      command += getJVMArguments() + " -Dport=" + port + " ";
      String serverCommand = command + ShutdownTestServer.class.getName() + " " + getTransport();
      serverCommand += " " + getServerArgs();
      String clientCommand = command + getClosingClientClassName() + " " + getTransport();
      clientCommand += " " + getClientArgs();
      
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
      Thread.sleep(15000);
      log.info("testing client");
      assertTrue(clientSuccessful);
      Thread.sleep(15000);
      log.info("testing server");
      assertTrue(serverSuccessful);
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * This test verifies that a server can shut down even if the client does not.
    */
   public void testOpenClient() throws Throwable
   {
      log.info("entering " + getName());
      
      port = PortUtil.findFreePort(InetAddress.getLocalHost().getHostName());
      String command = "java -cp \"" +  System.getProperty("java.class.path") + "\" ";
      command += getJVMArguments() + " -Dport=" + port + " ";
      String serverCommand = command + ShutdownTestServer.class.getName() + " " + getTransport();
      serverCommand += " " + getServerArgs();
      String clientCommand = command + OpenClient.class.getName() + " " + getTransport();
      clientCommand += " " + getClientArgs();
      
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
      Thread.sleep(40000);
      log.info("testing client");
      assertFalse(clientSuccessful);
      log.info("testing server");
      assertTrue(serverSuccessful);
      clientExecutor.destroy();
      log.info(getName() + " PASSES");
   }
   
   
   protected String getJVMArguments() throws Exception
   {
      return "";
   }
   
   
   protected String getServerArgs()
   {
      return "";
   }
   
   
   protected String getClientArgs()
   {
      return "";
   }
   
   
   protected String getHangingClientClassName()
   {
      return HangingClient.class.getName();
   }
   
   
   protected String getClosingClientClassName()
   {
      return ClosingClient.class.getName();
   }
   
   
   abstract protected String getTransport();
   
   
   public class Executor
   {
      private String command;
      private boolean server;
      private boolean successful;
      private Process process;
      private boolean ready;
      private Object lock = new Object();
      
      public Executor(String command, boolean server)
      {
         this.server = server;
         this.command = command;
      }
      
      public boolean successful()
      {
         return successful;
      }
      
      public void start() throws Exception
      {
         executeCommand(command);
      }
      
      private void executeCommand(String command) throws Exception
      {
         final String finalCommand = command;
         
         new Thread()
         {
            public void run()
            {
               try
               {
                  log.info("executing: " + finalCommand);
                  final Process local = Runtime.getRuntime().exec(finalCommand);
                  process = local;

                  final BufferedReader errStream = new BufferedReader(new InputStreamReader(local.getErrorStream()));
                  final BufferedReader inStream = new BufferedReader(new InputStreamReader(local.getInputStream()));
                  new Thread()
                  {
                     public void run()
                     {
                        try
                        {
                           String errOut = null;
                           while((errOut = errStream.readLine()) != null)
                           {
                              System.err.println(errOut);
                           }
                        }
                        catch(IOException e)
                        {
                        }
                     }
                  }.start();
                  new Thread()
                  {
                     public void run()
                     {
                        try
                        {
                           String stdOut = null;
                           while((stdOut = inStream.readLine()) != null)
                           {
                              System.out.println(stdOut);
                              if (stdOut.indexOf("READY") > -1)
                              {
                                 log.info("READY");
                                 synchronized (lock)
                                 {
                                    ready = true;
                                    lock.notify();
                                 }
                              }
                           }
                        }
                        catch(IOException e)
                        {
                        }
                     }
                  }.start();
                  
                  local.waitFor();
                  String clientOrServer = server ? "server" : "client";
                  log.info(clientOrServer + " exit value: " + local.exitValue());
                  successful = (local.exitValue() == 0);
                  if (server)
                     serverSuccessful = successful;
                  else
                     clientSuccessful = successful;
               }
               catch(Exception e)
               {
                  log.error("Error starting process: " + finalCommand, e);
               }
            }
         }.start();
      }
      
      public void waitUntilReady()
      {
         synchronized (lock)
         {
            while (!ready)
            {
               try
               {
                  lock.wait();
               }
               catch (InterruptedException e)
               {
               }
            }
         }
      }
        
      public void destroy()
      {
         process.destroy();
      }
   }
}