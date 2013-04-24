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
package org.jboss.remoting.samples.detection.jndi.ssl;

import java.util.HashMap;
import java.util.Map;

import org.jboss.remoting.samples.detection.jndi.SimpleDetectorServer;
import org.jboss.remoting.security.SSLSocketBuilder;

/** 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1480 $
 * <p>
 * Copyright Oct 9, 2006
 * </p>
 */
public class SimpleSSLDetectorServer extends SimpleDetectorServer
{
   
   public static void main(String[] args)
   {      
      println("Starting JBoss/Remoting server... to stop this server, kill it manually via Control-C");
      String locatorURI = getLocatorURI(args);
      println("This server's endpoint will be: " + locatorURI);
      
      SimpleDetectorServer server = new SimpleSSLDetectorServer();
      try
      {
         server.setupDetector();
         server.setupServer(locatorURI);
         
         // wait forever, let the user kill us at any point (at which point, the client will detect we went down)
         while(true)
         {
            Thread.sleep(1000);
         }
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
      
      println("Stopping JBoss/Remoting server");
   }
   
   protected Map getConfiguration()
   {
      Map config = new HashMap();
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, "JKS");
      String keyStoreFilePath = this.getClass().getResource("keystore").getFile();
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, keyStoreFilePath);
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "unit-tests-server");
      return config;
   }
}
