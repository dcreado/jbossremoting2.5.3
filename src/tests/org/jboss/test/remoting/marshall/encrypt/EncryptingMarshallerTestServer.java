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

package org.jboss.test.remoting.marshall.encrypt; 
 
import javax.management.MBeanServer;
import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.marshal.MarshalFactory;
import org.jboss.remoting.marshal.encryption.EncryptingMarshaller;
import org.jboss.remoting.marshal.encryption.EncryptingUnMarshaller;
import org.jboss.remoting.serialization.SerializationStreamFactory;
import org.jboss.remoting.transport.Connector;


/**
 * A EncryptingMarshallerTestServer.
 *
 * @author Anil.Saldhana@jboss.org
 * @version $Revision: 1402 $ 
 */

public class EncryptingMarshallerTestServer extends ServerTestCase
{
   private static final Object lock = new Object(); 

   public void setUp()
   {
      String javauri = "socket://localhost:5300/?datatype=encrypt"
                                      + "&"+ InvokerLocator.SERIALIZATIONTYPE + "=" +
                                      SerializationStreamFactory.JAVA_ENCRYPT;
      String jbosslocatorURI = "socket://localhost:5400/?datatype=encrypt"
                                      + "&"+ InvokerLocator.SERIALIZATIONTYPE + "=" +
                                      SerializationStreamFactory.JBOSS_ENCRYPT;
      EncryptionServerThread jbossThread = 
         new EncryptionServerThread(jbosslocatorURI,2);
      EncryptionServerThread javaThread = 
         new EncryptionServerThread(javauri,2);
      
      //Start the two threads - one for JBoss Invocation and one for Java invocation
      jbossThread.start();
      javaThread.start(); 
   } 

    

   public static void main(String[] args)
   {
      try
      {
         new EncryptingMarshallerTestServer().setUp();
      }
      catch (Exception e)
      { 
         e.printStackTrace();
      }
   }
   
   public class EncryptionServerThread extends Thread
   {
      private String locatorURI = null;
      private int counter = 0;
      public EncryptionServerThread(String url, int counter)
      {
         this.counter = counter;
         this.locatorURI = url;  
      }
      
      public void run()
      {
         try
         {
            
            MarshalFactory.addMarshaller("encrypt", 
                                         new EncryptingMarshaller(), 
                                         new EncryptingUnMarshaller()); 
            InvokerLocator locator = new InvokerLocator(locatorURI);
            System.out.println("Starting remoting server with locator uri of: " + locatorURI);
            Connector connector = new Connector();
            connector.setInvokerLocator(locator.getLocatorURI());
            connector.create();
            JavaInvocationHandler invocationHandler = new JavaInvocationHandler(counter);
            connector.addInvocationHandler("sample", invocationHandler);
            connector.start();

            synchronized(lock)
            {
               lock.wait();
            }

            connector.stop();
            connector.destroy();
         }
         catch(Exception e)
         {
            e.printStackTrace();
         }
      } 
   }
   
   
   /**
    * Noop Invocation Handler
    * @author <a href="anil.saldhana@jboss.com">Anil Saldhana</a>
    * @version $Revision: 1402 $
    */
   public abstract class AdapterInvocationHandler implements ServerInvocationHandler
   { 
      public void addListener(InvokerCallbackHandler callbackHandler)
      { 
      }

      public abstract Object invoke(InvocationRequest invocation) throws Throwable;

      public void removeListener(InvokerCallbackHandler callbackHandler)
      { 
      }

      public void setInvoker(ServerInvoker invoker)
      {  
      }

      public void setMBeanServer(MBeanServer server)
      { 
      } 
   }


   /**
    * Simple invocation handler implementation.
    * This is the code that will be called with the invocation payload from the client.
    */
   public class JavaInvocationHandler extends AdapterInvocationHandler
   { 
      private int counter = 0;
      public JavaInvocationHandler(int counter)
      {
         this.counter = counter; 
      }

      /**
       * called to handle a specific invocation
       *
       * @param invocation
       * @return
       * @throws Throwable
       */
      public Object invoke(InvocationRequest invocation) throws Throwable
      { 
         if(++counter == 2)
         { 
            synchronized(lock)
            {
               lock.notify();
            }
         }
         Object obj = invocation.getParameter(); 
         return obj;
      } 
   }
}

