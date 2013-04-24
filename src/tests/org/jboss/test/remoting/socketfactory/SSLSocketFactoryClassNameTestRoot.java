
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
package org.jboss.test.remoting.socketfactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.jboss.test.remoting.socketfactory.SocketFactoryClassNameTestRoot.TestSocketFactory;


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
public abstract class SSLSocketFactoryClassNameTestRoot extends SocketFactoryClassNameTestRoot
{
   public void setUp() throws Exception
   {
      super.setUp();
      String trustStoreFilePath = this.getClass().getResource("../.truststore").getFile();
      System.setProperty("javax.net.ssl.trustStore", trustStoreFilePath);
      System.setProperty("javax.net.ssl.trustStorePassword", "unit-tests-client");
      String keyStoreFilePath = this.getClass().getResource("../.keystore").getFile();
      System.setProperty("javax.net.ssl.keyStore", keyStoreFilePath);
      System.setProperty("javax.net.ssl.keyStorePassword", "unit-tests-server");
      System.setProperty("org.jboss.security.ignoreHttpsHost", "true");
   }
   
   protected Class getSocketFactoryClass()
   {
      return TestSSLSocketFactory.class;
   }
   
   public static class TestSSLSocketFactory extends SocketFactory
   {
      SocketFactory sf = SSLSocketFactory.getDefault();
      
      public TestSSLSocketFactory()
      {
      }

      public Socket createSocket() throws IOException, UnknownHostException
      {
         return sf.createSocket();
      }
      
      public Socket createSocket(String arg0, int arg1) throws IOException, UnknownHostException
      {
         return sf.createSocket(arg0, arg1);
      }

      public Socket createSocket(InetAddress arg0, int arg1) throws IOException
      {
         return sf.createSocket(arg0, arg1);
      }

      public Socket createSocket(String arg0, int arg1, InetAddress arg2, int arg3) throws IOException,
            UnknownHostException
      {
         return sf.createSocket(arg0, arg1, arg2, arg3);
      }

      public Socket createSocket(InetAddress arg0, int arg1, InetAddress arg2, int arg3) throws IOException
      {
         return sf.createSocket(arg0, arg1, arg2, arg3);
      } 
   }
}

