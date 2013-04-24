/*
* JBoss, Home of Professional Open Source
* Copyright 2009, JBoss Inc., and individual contributors as indicated
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
package org.jboss.test.remoting.ssl.emptystore;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.remoting.security.SSLSocketBuilder;


/**
 * Unit test for JBREM-1172.
 * 
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 * <p>
 * Copyright Dec 19, 2009
 * <p>
 * @version $Rev$
 */
public class EmptyStoreTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(EmptyStoreTestCase.class);
   
   private static boolean firstTime = true;

   
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
   }

   
   public void tearDown()
   {
   }
   
   
   public void testNONEKeyStore() throws Throwable
   {
      log.info("entering " + getName());
      
      // Create SSLSocketBuilder.
      HashMap config = new HashMap();
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, "JKS");
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, "NONE");
//      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "dummy");
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, "NONE");
//      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "dummy");
      SSLSocketBuilder socketBuilder = new SSLSocketBuilder(config);
      socketBuilder.setUseSSLServerSocketFactory(false);
      socketBuilder.setUseSSLSocketFactory(false);
      
      // Create ServerSocket.
      try
      {
      	ServerSocketFactory ssf = socketBuilder.createSSLServerSocketFactory();
      	ServerSocket ss = ssf.createServerSocket();
      	ss.close();
      }
      catch (IOException e)
      {
      	fail("Unable to create ServerSocket");
      }

      // Create Socket.
      try
      {
      	SocketFactory sf = socketBuilder.createSSLSocketFactory();
      	Socket s = sf.createSocket();
      	s.close();
      }
      catch (IOException e)
      {
      	fail("Unable to create Socket");
      }
      
      log.info(getName() + " PASSES");
   }
   
   /**
    * Creates "NONE" dynamically to test JBREM-1185.
    */
   public void testDistinctNONEKeyStore() throws Throwable
   {
      log.info("entering " + getName());
      
      // Create SSLSocketBuilder.
      HashMap config = new HashMap();
      String none = new StringBuffer("NONE").toString();
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, "JKS");
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, none);
//      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "dummy");
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, none);
//      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "dummy");
      SSLSocketBuilder socketBuilder = new SSLSocketBuilder(config);
      socketBuilder.setUseSSLServerSocketFactory(false);
      socketBuilder.setUseSSLSocketFactory(false);
      
      // Create ServerSocket.
      try
      {
        ServerSocketFactory ssf = socketBuilder.createSSLServerSocketFactory();
        ServerSocket ss = ssf.createServerSocket();
        ss.close();
      }
      catch (IOException e)
      {
        fail("Unable to create ServerSocket");
      }

      // Create Socket.
      try
      {
        SocketFactory sf = socketBuilder.createSSLSocketFactory();
        Socket s = sf.createSocket();
        s.close();
      }
      catch (IOException e)
      {
        fail("Unable to create Socket");
      }
      
      log.info(getName() + " PASSES");
   }
}