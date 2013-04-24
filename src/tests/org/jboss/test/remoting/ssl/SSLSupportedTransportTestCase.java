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
package org.jboss.test.remoting.ssl;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import junit.framework.TestCase;
import org.jboss.remoting.InvokerRegistry;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class SSLSupportedTransportTestCase extends TestCase
{
   public void testTransportSupportsSSL() throws Exception
   {
      final String[] transports = new String[] {"socket", "sslsocket", "multiplex", "sslmultiplex", "rmi", "sslrmi", "http", "https"};
      boolean[] expextedResult = new boolean[] {false, true, false, true, false, true, false, true};

      for(int x = 0; x < transports.length; x++)
      {
         Boolean isSupported = null;
         final int finalX = x;
         try
         {
            isSupported = (Boolean) AccessController.doPrivileged( new PrivilegedExceptionAction()
            {
               public Object run() throws Exception
               {
                  return new Boolean(InvokerRegistry.isSSLSupported(transports[finalX]));
               }
            });
         }
         catch (PrivilegedActionException pae)
         {
            throw pae.getException();
         }
         
         assertEquals("transport " + transports[x] + " was supposed to be " + expextedResult[x] + " for supporting ssl.", expextedResult[x], isSupported.booleanValue());
      }
   }
}