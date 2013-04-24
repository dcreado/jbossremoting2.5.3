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
package org.jboss.test.remoting.transport.socket.load;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.logging.XLevel;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transport.Connector;

public class SocketLoadTestCase extends TestCase
{
   private static int numOfRunnerThreads = 10;
   private static int responseCount = 0;
   private static Object lock = new Object();
   private Connector connector;

//   static
//   {
//      BasicConfigurator.configure();
//      Logger.getRootLogger().setLevel(Level.INFO);
//      Logger.getInstance("org.jboss.remoting.transport.socket").setLevel(Level.ALL);
//   }

   private static Logger logger = Logger.getLogger(SocketLoadTestCase.class);

   protected String getTransport()
   {
      return "socket";
   }
   
   public static void main(String[] args) throws Throwable
   {
      SocketLoadTestCase rt = new SocketLoadTestCase();
      rt.startServer();
//      rt.runMultipleClients(Integer.parseInt(args[1]));
      rt.runMultipleClients(numOfRunnerThreads);
      System.in.read();
   }

   public void setUp() throws Exception
   {
      logger.error("setUp()");
      Logger.getLogger("org.jboss.remoting").setLevel(XLevel.INFO);
      Logger.getLogger("org.jboss.test.remoting").setLevel(Level.INFO);
      String pattern = "[%d{ABSOLUTE}] [%t] %5p (%F:%L) - %m%n";
      PatternLayout layout = new PatternLayout(pattern);
      ConsoleAppender consoleAppender = new ConsoleAppender(layout);
      Logger.getRootLogger().addAppender(consoleAppender); 
      
      logger.info("starting server");
      startServer();
   }

   public void tearDown() throws Exception
   {
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   public void startServer() throws Exception
   {
      logger.info("startServer()");
      String locatorURI = getTransport() + "://localhost:54000/?maxPoolSize=2&timeout=10000";
      InvokerLocator locator = new InvokerLocator(locatorURI);

      connector = new Connector();

      connector.setInvokerLocator(locator.getLocatorURI());
      connector.create();

      SampleInvocationHandler invocationHandler = new SampleInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
   }

   public void testRunClients() throws Throwable
   {
      runMultipleClients(numOfRunnerThreads);
      Thread.currentThread().sleep(120000);
      logger.info("Response count = " + responseCount + ".  Expected 10.");
      assertEquals(10, responseCount);
   }

   public void runClient(String clientId) throws Throwable
   {
      String locatorURI = getTransport() + "://localhost:54000/";
      InvokerLocator locator = new InvokerLocator(locatorURI);
      Client client = new Client(locator);
      client.connect();
      String req = clientId;
      logger.info("client.invoke() " + clientId);
      Object resp = client.invoke(req);
      
      synchronized (lock)
      {
         responseCount++;
      }
      
      logger.info("Received response of: " + resp + ".  Response count = " + responseCount);
      System.in.read();
   }

      public void runMultipleClients(int cnt) throws Throwable {
      for (int i = 0; i < cnt; i++) {
         Thread t = new Thread(new Runnable() {
            public void run() {
               try {
                  Thread.sleep(1000);
                  runClient(Thread.currentThread().getName());
               } catch (Throwable e) {
                  logger.error(e);
               }
            }
         }, Integer.toString(i));
         t.start();
      }
   }
}