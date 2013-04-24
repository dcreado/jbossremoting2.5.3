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

package org.jboss.test.remoting.transport.socket.timeout.keepalive;

import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;

import java.net.SocketTimeoutException;
import java.rmi.MarshalException;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class TimeoutClientTest
{
   //private String locatorURI = "socket://localhost:8899/?socketTimeout=1000&backlog=500&clientMaxPoolSize=100&maxPoolSize=500";
   //private String locatorURI = "socket://localhost:8899/?socketTimeout=1000&backlog=0&clientMaxPoolSize=10&maxPoolSize=3";
   private String locatorURI = "socket://localhost:8899/?socketTimeout=1000&backlog=0&clientMaxPoolSize=10&maxPoolSize=3&serializationtype=jboss";

   private Client client = null;

   public void testTimeout() throws Exception
   {
      client = new Client(new InvokerLocator(locatorURI));
      client.connect();

      for(int x = 0; x < 50; x++)
      {
         new Thread(new Runnable()
         {
            public void run()
            {
               try
               {
                  makeCall();
               }
               catch(Exception e)
               {
                  e.printStackTrace();
               }
            }
         }).start();
      }

   }

   public void makeCall() throws Exception
   {

      //test for client timeout
      try
      {
         client.invoke("foo");
         client.invoke("foo");
         client.invoke("foo");
         client.invoke("foo");

         Thread.currentThread().sleep(2000);
         client.invoke("bar");
         client.invoke("bar");
         client.invoke("bar");

         Thread.currentThread().sleep(1000);
         client.invoke("bar");
         client.invoke("bar");
         Object ret = client.invoke("bar");

         System.out.println("Done making all calls after sleeping.  Return is " + ret);
      }
      catch(Throwable throwable)
      {
         if(throwable instanceof MarshalException)
         {
            Throwable cause = throwable.getCause();
            if(cause instanceof SocketTimeoutException)
            {
               System.out.println("Got socket timeout exception - " + cause.getMessage());
               return;
            }
         }
         if(throwable instanceof Exception)
         {
            throw (Exception) throwable;
         }
         else
         {
            throw new Exception(throwable);
         }
      }

   }

   public static void main(String[] args)
   {

//      org.apache.log4j.BasicConfigurator.configure();
//      org.apache.log4j.Category.getInstance("org.jboss.remoting").setLevel(XLevel.TRACE);
      //org.apache.log4j.Category.getRoot().setLevel(XLevel.TRACE);

      TimeoutClientTest client = new TimeoutClientTest();
      try
      {
         client.testTimeout();
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }

}