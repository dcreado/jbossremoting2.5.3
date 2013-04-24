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

package org.jboss.test.remoting.stream;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class StreamingTestClient extends TestCase
{
   private static Logger log = Logger.getLogger(StreamingTestClient.class);
   
   // Default locator values
   private static String transport = "socket";
   private static String host = "localhost";
   private static int port = 5400;

   private String locatorURI;
   private Client remotingClient = null;
   private File testFile = null;
   private FileInputStream fileInput = null;

   private boolean error = false;

   public void testStream() throws Throwable
   {
      for(int x = 0; x < 5; x++)
      {
//         new Thread(new Runnable() {
//            public void run()
//            {
//               try
//               {
                  sendStream();
//               }
//               catch (Throwable throwable)
//               {
//                  throwable.printStackTrace();
//                  error = true;
//               }
//            }
//         }).start();
      }

//      Thread.sleep(5000);
//      assertFalse(error);
      
      log.info("Waiting to give server a chance to send ");
      log.info("org.jboss.remoting.stream.StreamHandler.CLOSE message");
      Thread.sleep(5000);
   }

   public void sendStream() throws Throwable
   {
      URL fileURL = this.getClass().getResource("test.txt");
      if(fileURL == null)
      {
         throw new Exception("Can not find file test.txt");
      }
      testFile = new File(fileURL.getFile());
      fileInput = new FileInputStream(testFile);

      String param = "foobar";
      long fileLength = testFile.length();
      log.info("File size = " + fileLength);
      Object ret = remotingClient.invoke(fileInput, param);

      Map responseMap = (Map)ret;
      String subSys = (String)responseMap.get("subsystem");
      String clientId = (String)responseMap.get("clientid");
      String paramVal = (String)responseMap.get("paramval");

      assertEquals("test_stream".toUpperCase(), subSys);
      assertEquals(remotingClient.getSessionId(), clientId);
      assertEquals("foobar", paramVal);

      Object response = remotingClient.invoke("get_size");
      int returnedFileLength = ((Integer) response).intValue();
      log.info("Invocation response: " + response);

      if(fileLength == returnedFileLength)
      {
         log.info("PASS");
      }
      else
      {
         log.info("FAILED - returned file length was " + returnedFileLength);
      }
      assertEquals(fileLength, returnedFileLength);
   }

   public void setUp() throws Exception
   {
      String bindAddr = System.getProperty("jrunit.bind_addr", host);
      locatorURI = transport + "://" + bindAddr + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      log.info("Calling remoting server with locator uri of: " + locatorURI);

      remotingClient = new Client(locator, "test_stream");
      remotingClient.connect();
   }

   public void tearDown() throws Exception
   {
      if(remotingClient != null)
      {
         remotingClient.disconnect();
      }
      if(fileInput != null)
      {
         fileInput.close();
      }
   }

   /**
    * Can pass transport and port to be used as parameters.
    * Valid transports are 'rmi' and 'socket'.
    *
    * @param args
    */
   public static void main(String[] args)
   {
      if(args != null && args.length == 2)
      {
         transport = args[0];
         port = Integer.parseInt(args[1]);
      }
      String locatorURI = transport + "://" + host + ":" + port;
      StreamingTestClient client = new StreamingTestClient();
      try
      {
         client.setUp();
         client.testStream();
         client.tearDown();
      }
      catch(Throwable e)
      {
         e.printStackTrace();
      }
   }


}