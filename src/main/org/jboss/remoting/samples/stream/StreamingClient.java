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

import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

/**
 * This samples shows how the client can send an InputStream to
 * the server.
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class StreamingClient
{
   // Default locator values
   private static String transport = "socket";
   private static String host = "localhost";
   private static int port = 5400;

   private String locatorURI = transport + "://" + host + ":" + port;

   private String localFileName = "sample.txt";
   private String remoteFileName = "server_sample.txt";

   public void sendStream() throws Throwable
   {
      FileInputStream fileInput = null;
      Client remotingClient = null;

      try
      {
         InvokerLocator locator = new InvokerLocator(locatorURI);
         System.out.println("Calling on remoting server with locator uri of: " + locatorURI);

         remotingClient = new Client(locator);
         remotingClient.connect();

         URL fileURL = this.getClass().getResource(localFileName);
         System.out.println("looking for file at " + fileURL);
         if(fileURL == null)
         {
            throw new Exception("Can not find file " + localFileName);
         }
         File testFile = new File(fileURL.getFile());
         fileInput = new FileInputStream(testFile);

         System.out.println("Sending input stream for file " + localFileName + " to server.");
         Object ret = remotingClient.invoke(fileInput, remoteFileName);

         long fileLength = testFile.length();
         System.out.println("Size of file sample.txt is " + fileLength);
         System.out.println("Server returned " + ret + " as the size of the file read.");

      }
      finally
      {
         if(remotingClient != null)
         {
            remotingClient.disconnect();
         }
         if(fileInput != null)
         {
            try
            {
               fileInput.close();
            }
            catch(IOException e)
            {
               e.printStackTrace();
            }
         }
      }
   }

   public void setRemoteFileName(String remoteFileName)
   {
      this.remoteFileName = remoteFileName;
   }


   /**
    * Can pass transport and port to be used as parameters.
    * Valid transports are 'rmi' and 'socket'.
    *
    * @param args
    */
   public static void main(String[] args)
   {
      String newFileName = null;
      if(args != null && args.length == 1)
      {
         newFileName = args[0];
      }
      if(args != null && args.length == 2)
      {
         transport = args[0];
         port = Integer.parseInt(args[1]);
      }
      if(args != null && args.length == 3)
      {
         transport = args[0];
         port = Integer.parseInt(args[1]);
         newFileName = args[2];
      }
      String locatorURI = transport + "://" + host + ":" + port;
      StreamingClient client = new StreamingClient();
      if(newFileName != null)
      {
         client.setRemoteFileName(newFileName);
      }
      try
      {
         client.sendStream();
      }
      catch(Throwable e)
      {
         e.printStackTrace();
      }
   }


}