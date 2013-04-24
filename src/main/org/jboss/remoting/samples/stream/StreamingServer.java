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

package org.jboss.remoting.samples.stream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import javax.management.MBeanServer;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.stream.StreamInvocationHandler;
import org.jboss.remoting.transport.Connector;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class StreamingServer
{
   // Default locator values
   private static String transport = "socket";
   private static String host = "localhost";
   private static int port = 5400;

   private String locatorURI = transport + "://" + host + ":" + port;
   private Connector connector = null;

   public void setupServer() throws Exception
   {
      InvokerLocator locator = new InvokerLocator(locatorURI);
      System.out.println("Starting remoting server with locator uri of: " + locatorURI);
      connector = new Connector();
      connector.setInvokerLocator(locator.getLocatorURI());
      connector.create();

      TestStreamInvocationHandler invocationHandler = new TestStreamInvocationHandler();
      // first parameter is sub-system name.  can be any String value.
      connector.addInvocationHandler("test_stream", invocationHandler);

      connector.start(true);
   }


   /**
    * Can pass transport and port to be used as parameters.
    * Valid transports are 'rmi' and 'socket'.
    *
    * @param args
    */
   public static void main(String[] args)
   {
      if(args != null && args.length == 3)
      {
         transport = args[0];
         host = args[1];
         port = Integer.parseInt(args[2]);
      }

      StreamingServer server = new StreamingServer();
      try
      {
         server.setupServer();

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
   }

   /**
    * Simple invocation handler implementation.
    */
   public static class TestStreamInvocationHandler implements StreamInvocationHandler
   {
      private long streamSize = 0;

      /**
       * takes the incoming stream and writes out to a file specified by the other param specified.
       * will return the size of the file.
       *
       * @param stream
       * @param param
       * @return
       */
      public Object handleStream(InputStream stream, InvocationRequest param)
      {
         try
         {
            String fileName = (String)param.getParameter();
            System.out.println("Received input stream from client to write out to file " + fileName);
            File newFile = new File(fileName);
            if(!newFile.exists())
            {
               newFile.createNewFile();
            }

            FileOutputStream out = new FileOutputStream(newFile, false);

            byte buf[] = new byte[4096];
            while(true)
            {
               int c = stream.read(buf);
               if(c < 0)
               {
                  break;
               }
               out.write(buf, 0, c);
            }

            out.flush();
            out.close();

            streamSize = newFile.length();
            System.out.println("New file " + fileName + " has been written out to " + newFile.getAbsolutePath());
            System.out.println("Size of " + newFile.getAbsolutePath() + " is " + streamSize);
         }
         catch(Throwable e)
         {
            e.printStackTrace();
         }
         finally
         {
            try
            {
               stream.close();
            }
            catch(IOException e)
            {
               e.printStackTrace();
            }
         }
         return new Long(streamSize);
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
         // Print out the invocation request
         System.out.println("Invocation request is: " + invocation.getParameter());

         // Return the size of the file already streamed to the server (and written to disk).
         return new Long(streamSize);
      }

      /**
       * Adds a callback handler that will listen for callbacks from
       * the server invoker handler.
       *
       * @param callbackHandler
       */
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         // NO OP as do not handling callback listeners in this example
      }

      /**
       * Removes the callback handler that was listening for callbacks
       * from the server invoker handler.
       *
       * @param callbackHandler
       */
      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         // NO OP as do not handling callback listeners in this example
      }

      /**
       * set the mbean server that the handler can reference
       *
       * @param server
       */
      public void setMBeanServer(MBeanServer server)
      {
         // NO OP as do not need reference to MBeanServer for this handler
      }

      /**
       * set the invoker that owns this handler
       *
       * @param invoker
       */
      public void setInvoker(ServerInvoker invoker)
      {
         // NO OP as do not need reference back to the server invoker
      }
   }
}