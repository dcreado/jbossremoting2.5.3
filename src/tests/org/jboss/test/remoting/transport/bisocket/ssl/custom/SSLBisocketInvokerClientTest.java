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

package org.jboss.test.remoting.transport.bisocket.ssl.custom;

import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.bisocket.Bisocket;
import org.jboss.test.remoting.transport.socket.ssl.custom.InvokerClientTest;


/**
 * @author <a href="mailto:tom@jboss.com">Tom Elrod</a>
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 3591 $
 * <p>
 * Copyright (c) Dec 15, 2006
 * </p>
 */
public class SSLBisocketInvokerClientTest extends InvokerClientTest
{
   private static Logger log = Logger.getLogger(SSLBisocketInvokerClientTest.class);
   
   public void testCallbacks()
   {
      try
      {
         InvokerLocator locator = new InvokerLocator(getTransport() + "://localhost:" + callbackPort);
         HashMap config = new HashMap();
         config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
         String trustStoreFilePath = this.getClass().getResource("../.truststore").getFile();
         config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStoreFilePath);
         config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");
         config.put(Bisocket.IS_CALLBACK_SERVER, "true");
         Connector callbackConnector = new Connector(locator.getLocatorURI(), config);
         callbackConnector.setServerSocketFactory(createServerSocketFactory());
         callbackConnector.create();
         callbackConnector.addInvocationHandler("sample", new SampleInvocationHandler());
         callbackConnector.start();

         CallbackHandler callbackHandler = new CallbackHandler();
         String callbackHandleObject = "myCallbackHandleObject";
         client.addListener(callbackHandler, locator, callbackHandleObject);
         solicitCallback("abc");
         
         // need to wait for brief moment so server can callback
         Thread.sleep(1000);
         
         // remove callback handler from server
         client.removeListener(callbackHandler);
         
         // shut down callback server
         callbackConnector.stop();
         callbackConnector.destroy();
         callbackConnector = null;
         
         List callbacks = callbackHandler.getCallbacks();
         assertEquals(callbacks.size(), 1);
//         assertEquals(callbacks.get(0), "abc");
      }
      catch (Throwable t)
      {
         log.error(t);
         t.printStackTrace();
         fail();
      }
   }
   
   protected String getTransport()
   {
      return "sslbisocket";
   }
}
