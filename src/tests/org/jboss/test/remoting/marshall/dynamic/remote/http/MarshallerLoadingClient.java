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

package org.jboss.test.remoting.marshall.dynamic.remote.http;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class MarshallerLoadingClient extends TestCase implements MarshallerLoadingConstants
{
   public void testInvocation() throws Throwable
   {
      Client remotingClient = null;

      // first, make sure can not load marshaller from current classpath
      try
      {
         Class.forName("org.jboss.test.remoting.marshall.dynamic.remote.http.TestMarshaller");
         assertTrue("Was able to load the TestMarshaller class locally, when it should NOT be on the classpath.", false);
         Class.forName("org.jboss.test.remoting.marshall.dynamic.remote.http.TestObject");
         assertTrue("Was able to load the TestObject class locally, when it should NOT be on the classpath.", false);
         return;
      }
      catch(ClassNotFoundException e)
      {
         assertTrue("Was not able to load TestMarshaller from local classpath as expected.", true);
      }

      try
      {
         String newLocatorURI = locatorURI;
         String metadata = System.getProperty("remoting.metadata");
         if(metadata != null)
         {
            newLocatorURI += "&" + metadata;
         }
         InvokerLocator locator = new InvokerLocator(newLocatorURI);
         System.out.println("Calling remoting server with locator uri of: " + newLocatorURI);

         remotingClient = new Client(locator);
         remotingClient.connect();
         Object response = remotingClient.invoke("Do something", null);
         assertTrue(true);

         System.out.println("Invocation response: " + response);
      }
      finally
      {
         if(remotingClient != null)
         {
            remotingClient.disconnect();
         }
      }
   }

   public static void main(String[] args)
   {
      MarshallerLoadingClient client = new MarshallerLoadingClient();
      try
      {
         client.testInvocation();
      }
      catch(Throwable throwable)
      {
         throwable.printStackTrace();
      }
   }

   /**
    * Can pass transport and port to be used as parameters.
    * Valid transports are 'rmi' and 'socket'.
    *
    * @param args
    */
//   public static void main(String[] args)
//   {
//      org.apache.log4j.BasicConfigurator.configure();
//      org.apache.log4j.Category.getRoot().setLevel(Level.INFO);
//      org.apache.log4j.Category.getInstance("org.jboss.remoting").setLevel(Level.DEBUG);
//
//      /*
//      if(args != null && args.length == 2)
//      {
//         transport = args[0];
//         port = Integer.parseInt(args[1]);
//      }
//      */
//      MarshallerLoadingClient client = new MarshallerLoadingClient(MarshallerLoadingClient.class.getName());
//      try
//      {
//         MultipleTestRunner runner = new MultipleTestRunner();
//         runner.doRun(client, true);
//      }
//      catch(Throwable e)
//      {
//         e.printStackTrace();
//         System.exit(1);
//      }
//      System.exit(0);
//   }
//

}