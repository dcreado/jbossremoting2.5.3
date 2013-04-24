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

package org.jboss.test.remoting.performance.raw.rmi;

import EDU.oswego.cs.dl.util.concurrent.Latch;
import junit.framework.Test;
import org.jboss.jrunit.decorators.ThreadLocalDecorator;
import org.jboss.logging.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.test.remoting.performance.synchronous.MultiThreadedPerformanceClientTest;
import org.jboss.test.remoting.performance.synchronous.PerformanceCallbackKeeper;

import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UID;
import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class MultiThreadedRMIPerformanceClient extends MultiThreadedPerformanceClientTest
{
   private int rmiPort = 1099;
   private RMIServerRemote rmiServer;
   private String clientSessionId = new UID().toString();

   protected static final Logger log = Logger.getLogger(MultiThreadedRMIPerformanceClient.class);

   public static Test suite()
   {
      return new ThreadLocalDecorator(MultiThreadedRMIPerformanceClient.class, 1);
   }

   public void init()
   {
      //super.init();

      String name = "//" + host+ "/RMIServer";
//         RMIServer svr = (RMIServer) Naming.lookup(name);

      try
      {
         //Registry regsitry = LocateRegistry.getRegistry("localhost", rmiPort);
         Registry regsitry = LocateRegistry.getRegistry(rmiPort);
         Remote remoteObj = regsitry.lookup(name);
         rmiServer = (RMIServerRemote) remoteObj;
      }
      catch(Exception e)
      {
         log.error("Error initializating rmi client.", e);
      }
   }

   /**
    * This will be used to create callback server
    *
    * @param port
    * @return
    * @throws Exception
    */
   protected InvokerLocator initServer(int port) throws Exception
   {
      return null;
   }

   protected PerformanceCallbackKeeper addCallbackListener(String sessionId, Latch serverDoneLock)
         throws Throwable
   {
      RMICallbackServer callbackServer = new RMICallbackServer(clientSessionId, serverDoneLock);
      callbackServer.start();
      return callbackServer;
   }

   protected void populateMetadata(Map metadata)
   {
      super.populateMetadata(metadata);
      metadata.put("transport", "raw_rmi");
      metadata.put("serialization", "java");
   }

   protected Object getBenchmarkAlias()
   {
      String config = System.getProperty("alias");
      if(config == null || config.length() == 0)
      {
         config = System.getProperty("jboss-junit-configuration");
         if(config == null || config.length() == 0)
         {
            config = "raw_rmi" + "_" + getNumberOfCalls() + "_" + getPayloadSize() + "_" + "java";
         }
      }
      return config;
   }


   protected Object makeInvocation(String method, Object param) throws Throwable
   {
      if(method.equals(NUM_OF_CALLS))
      {
         return rmiServer.sendNumberOfCalls(clientSessionId, param);
      }
      else if(method.equals(TEST_INVOCATION))
      {
         return rmiServer.makeCall(clientSessionId, param);
      }
      else
      {
         throw new Exception("Was not able to find remote method call for " + method);
      }
   }

}