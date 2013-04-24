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
package org.jboss.test.remoting.timeout;

import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;

/** 
 * PerInvocationTimeoutTestRoot is the abstract parent of a set of per-transport
 * unit tests that verify the correctness of the facility for setting the socket
 * timeout value with each invocation.
 * 
 * See JIRA issue JBREM-598: http://jira.jboss.com/jira/browse/JBREM-598
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2255 $
 * <p>
 * Copyright Jan 24, 2007
 * </p>
 */
public abstract class PerInvocationTimeoutTestRoot extends TestCase
{
   protected static final String NO_WAIT    = "nowait";
   protected static final String SHORT_WAIT = "shortwait";
   protected static final String LONG_WAIT  = "longwait";
   protected static final String CONFIGURED_TIMEOUT_STRING = "8000";
   protected static final int    CONFIGURED_TIMEOUT = 8000;
   
   protected static Logger log = Logger.getLogger(PerInvocationTimeoutTestRoot.class);
   protected static boolean firstTime = true;
   
   protected Connector connector;
   protected Client client;
   
   
   public void setUp() throws Exception
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
      
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      HashMap serverConfig = new HashMap();
      addServerConfig(serverConfig);
      connector = new Connector(locator, serverConfig);
      connector.create();
      connector.addInvocationHandler("test", new TestHandler());
      connector.start();
      
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(ServerInvoker.TIMEOUT, CONFIGURED_TIMEOUT_STRING);
      addClientConfig(clientConfig);
      client = new Client(locator, clientConfig);
      client.connect();
   }

   
   public void testTimeout() throws Throwable
   {
      log.info("entering " + getName());
      
      Object response = client.invoke(SHORT_WAIT);
      assertEquals(SHORT_WAIT, response);
      log.info("invocation successful");
      
      try
      {
         log.info("================ EXPECTING EXCEPTION ================");
         client.invoke(LONG_WAIT);
         log.info("========= DIDN'T GET EXPECTED EXCEPTION ===========");
         fail("didn't get expected timeout: initial timeout");
      }
      catch (SocketTimeoutException e)
      {
         log.info("================ GOT EXPECTED EXCEPTION =============");
      }
      catch (Exception e)
      {
         if (e.getCause() instanceof SocketTimeoutException)
         {
            log.info("================ GOT EXPECTED EXCEPTION =============");
         }
         else
         {
            log.info(e);
            log.info("================ GOT UNEXPECTED EXCEPTION =============");  
            fail();
         }
      }
      
      try
      {
         log.info("================ EXPECTING EXCEPTION ================");
         HashMap metadata = new HashMap();
         metadata.put(ServerInvoker.TIMEOUT, "1000");
         client.invoke(SHORT_WAIT, metadata);
         log.info("========= DIDN'T GET UNEXPECTED EXCEPTION ==========="); 
         fail("didn't get expected timeout: initial timeout");
      }
      catch (SocketTimeoutException e)
      {
         log.info("================ GOT EXPECTED EXCEPTION =============");
      }
      catch (Exception e)
      {
         if (e.getCause() instanceof SocketTimeoutException)
         {
            log.info("================ GOT EXPECTED EXCEPTION =============");
         }
         else
         {
            log.info(e);
            e.printStackTrace();
            log.info("================ GOT UNEXPECTED EXCEPTION =============");  
            fail();
         }
      }
      
      // Make sure timeout was reset after previous invocation.  (Actually, this
      // test is transport dependent, but it's included here for completeness.  A
      // version of this test could be supplied for each transport.)
      response = client.invoke(SHORT_WAIT);
      assertEquals(SHORT_WAIT, response);
      log.info("invocation successful");
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   abstract protected String getTransport();
   
   
   protected void addServerConfig(Map config)
   {  
   }
   
   
   protected void addClientConfig(Map config)
   {  
   }
   
   
   public class TestHandler implements ServerInvocationHandler
   {

      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         String command = (String) invocation.getParameter();
         if (NO_WAIT.equals(command))
         {
            return command;
         }
         else if (SHORT_WAIT.equals(command))
         {
            Thread.sleep(4000);
            return command;
         }
         else if (LONG_WAIT.equals(command))
         {
            Thread.sleep(12000);
            return command;
         }
         else
         {
            return command;
         }
      }

      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
   }
}
