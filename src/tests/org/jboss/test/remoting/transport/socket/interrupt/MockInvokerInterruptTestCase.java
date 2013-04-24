/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.test.remoting.transport.socket.interrupt;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.jboss.remoting.CannotConnectException;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.transport.socket.MicroSocketClientInvoker;
import org.jboss.remoting.transport.socket.SocketWrapper;

import EDU.oswego.cs.dl.util.concurrent.CountDown;

/**
 * Unit test for JBREM-955.
 * 
 * @author <a href="mailto:galder.zamarreno@jboss.com">Galder Zamarreno</a>
 */
public class MockInvokerInterruptTestCase extends TestCase
{
   private static final Logger log = Logger.getLogger(MockInvokerInterruptTestCase.class);
   
   public void test000() throws Throwable
   {
      InvokerLocator il = new InvokerLocator("unittest", "127.0.0.1", 9999, "mock", null);
      CountDown startGate = new CountDown(1);
      MockMicroSocketClientInvoker ci = new MockMicroSocketClientInvoker(il, startGate);
      InvocationRequest ir = new InvocationRequest("", "", null, null, null, il);
      
      Runnable interrupterRunnable = new ThreadInterrupter(Thread.currentThread(), startGate);
      Thread interrupter = new Thread(interrupterRunnable);
      interrupter.start();
      
      ci.setMaxPoolSize(0);
      ci.connect();
      try
      {
         ci.invoke(ir);
      }
      catch(CannotConnectException cce)
      {
         log.error("We interrupted the connection, a more meaningul exception should be thrown");
         throw cce;
      }
      catch (RuntimeException re)
      {
         assertTrue(re.getCause() instanceof InterruptedException);
      }
   }

   class MockMicroSocketClientInvoker extends MicroSocketClientInvoker
   {
      private CountDown startGate;
      
      public MockMicroSocketClientInvoker(InvokerLocator locator, CountDown start)
      {
         super(locator);
         startGate = start;
      }

      public void setMaxPoolSize(int maxPoolSize)
      {
         this.maxPoolSize = maxPoolSize;
      }

      protected SocketWrapper getConnection(Marshaller marshaller, UnMarshaller unmarshaller,
                                            boolean tryPool, int timeAllowed)
            throws Exception
      {
         log.info("Request a connection but before that, let's open the start gate");
         startGate.release();
         return super.getConnection(marshaller, unmarshaller, true, timeAllowed);
      }
   }
   
   class ThreadInterrupter implements Runnable
   {
      private Thread threadToInterrupt;
      
      private CountDown startGate;
      
      ThreadInterrupter(Thread thread, CountDown start)
      {
         threadToInterrupt = thread;
         startGate = start;
      }

      public void run()
      {
         try
         {
            log.info("Wait for start gate to be opened");
            startGate.acquire();
            
            log.info("Start gate opened, let's sleep briefly...");
            Thread.sleep(200);
            
            log.info("Sleep finished, interrupt the target thread");
            threadToInterrupt.interrupt();
         }
         catch (InterruptedException e)
         {
            log.error("Error", e);
         }
      }
      
   }
}
