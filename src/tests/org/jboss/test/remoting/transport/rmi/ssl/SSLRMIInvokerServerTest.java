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

package org.jboss.test.remoting.transport.rmi.ssl;

import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.test.remoting.transport.InvokerServerTest;
import org.apache.log4j.Level;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class SSLRMIInvokerServerTest extends InvokerServerTest
{
   public String getTransport()
   {
      return "sslrmi";
   }
   
   public void setUp() throws Exception
   {
      System.setProperty(SSLSocketBuilder.STANDARD_KEY_STORE_TYPE, "JKS");
      String keyStoreFilePath = getClass().getResource(".keystore").getFile();
      System.setProperty(SSLSocketBuilder.STANDARD_KEY_STORE_FILE_PATH, keyStoreFilePath);
      System.setProperty(SSLSocketBuilder.STANDARD_KEY_STORE_PASSWORD, "unit-tests-server");

      System.setProperty(SSLSocketBuilder.STANDARD_TRUST_STORE_TYPE, "JKS");
      String trustStoreFilePath = getClass().getResource(".truststore").getFile();
      System.setProperty(SSLSocketBuilder.STANDARD_TRUST_STORE_FILE_PATH, trustStoreFilePath);
      System.setProperty(SSLSocketBuilder.STANDARD_TRUST_STORE_PASSWORD, "unit-tests-client");

      super.setUp();
   }

   public static void main(String[] args)
   {
      org.apache.log4j.BasicConfigurator.configure();
      org.apache.log4j.Category.getRoot().setLevel(Level.INFO);
      
      InvokerServerTest server = new SSLRMIInvokerServerTest();
      try
      {
         server.setUp();
         Thread.currentThread().sleep(30000);
         server.tearDown();
         System.out.println("Have torn down test.");
         Thread.currentThread().sleep(30000);
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }

}