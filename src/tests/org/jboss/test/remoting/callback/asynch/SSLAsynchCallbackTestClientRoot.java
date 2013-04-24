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
package org.jboss.test.remoting.callback.asynch;

import java.util.Map;

import org.jboss.remoting.security.SSLSocketBuilder;

/** 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2103 $
 * <p>
 * Copyright Feb 3, 2007
 * </p>
 */
public abstract class SSLAsynchCallbackTestClientRoot extends AsynchCallbackTestClientRoot
{  
   protected void addTransportSpecificConfig(Map config)
   {
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
      String trustStoreFilePath = this.getClass().getResource("truststore").getFile();
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStoreFilePath);
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");  
      
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, "JKS");
      String keyStoreFilePath = this.getClass().getResource("keystore").getFile();
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, keyStoreFilePath);
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "unit-tests-server"); 
      
      config.put(SSLSocketBuilder.REMOTING_SERVER_SOCKET_USE_CLIENT_MODE, "true");
      config.put(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE, "true");
   }
}
