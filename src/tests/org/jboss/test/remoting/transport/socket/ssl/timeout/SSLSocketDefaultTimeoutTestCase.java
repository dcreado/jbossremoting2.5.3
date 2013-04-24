/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.test.remoting.transport.socket.ssl.timeout;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transport.ClientFactory;
import org.jboss.remoting.transport.ClientInvoker;
import org.jboss.remoting.transport.socket.ServerAddress;
import org.jboss.remoting.transport.sslsocket.SSLSocketClientInvoker;
import org.jboss.test.remoting.transport.socket.timeout.SocketDefaultTimeoutTestCase;


/**
 * Unit tests for JBREM-1188.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Feb 16, 2010
 */
public class SSLSocketDefaultTimeoutTestCase extends SocketDefaultTimeoutTestCase
{
   private static Logger log = Logger.getLogger(SSLSocketDefaultTimeoutTestCase.class);   
   
   
   protected Class getClientFactoryClass()
   {
      return TestSSLSocketClientFactory.class;
   }
   
   
   protected Class getClientInvokerClass()
   {
      return TestSSLSocketClientInvoker.class;
   }
   
   
   public static class TestSSLSocketClientFactory implements ClientFactory
   {
      public ClientInvoker createClientInvoker(InvokerLocator locator, Map config) throws IOException
      {
         ClientInvoker clientInvoker = new TestSSLSocketClientInvoker(locator, config);
         log.info("TestClientFaotory.createClientInvoker() returning " + clientInvoker);
         return clientInvoker;
      }
      public boolean supportsSSL()
      {
         return true;
      }  
   }
   
   
   public static class TestSSLSocketClientInvoker extends SSLSocketClientInvoker
   {
      public TestSSLSocketClientInvoker(InvokerLocator locator, Map configuration) throws IOException
      {
         super(locator, configuration);
      }
      public TestSSLSocketClientInvoker(InvokerLocator locator) throws IOException
      {
         super(locator);
      }
      public ServerAddress getServerAddress()
      {
         return address;
      }
      public String toString()
      {
         return "TestSSLSocketClientInvoker";
      }
   }
}