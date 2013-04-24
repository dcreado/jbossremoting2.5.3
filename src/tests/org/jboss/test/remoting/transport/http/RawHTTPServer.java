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

package org.jboss.test.remoting.transport.http;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class RawHTTPServer
{
   private ServerSocket socket;
   private int port;
   private boolean started = false;
   private List clients = Collections.synchronizedList(new ArrayList());

   private static int clientCount = 0;

   private static final int DEFAULT_PORT = 80;
   private Listener listener;

   public RawHTTPServer()
   {
      this(DEFAULT_PORT);
   }

   public RawHTTPServer(int port)
   {
      this.port = port;
   }

   public void start()
   {
      if(!started)
      {
         System.out.println("Starting raw http server.");
         try
         {
            this.socket = new ServerSocket(this.port);
            this.port = socket.getLocalPort();
            this.started = true;
            this.listener = new Listener();
            this.listener.start();
            System.out.println("Server started.");
         }
         catch(IOException e)
         {
            System.out.println("Server start failed.");
            e.printStackTrace();
         }
      }
   }

   public synchronized void stop()
   {
      if(started)
      {
         this.started = false;
         this.listener.interrupt();
         try
         {
            this.listener.join(500);
         }
         catch(Throwable ig)
         {
         }
         Iterator iter = clients.iterator();
         while(iter.hasNext())
         {
            Client client = (Client) iter.next();
            client.interrupt();
         }
         clients.clear();
         try
         {
            this.socket.close();
         }
         catch(Throwable ig)
         {
         }
         this.socket = null;
         this.listener = null;
      }
   }


   private final class Client extends Thread
   {
      Socket socket;
      InputStream input;
      OutputStream output;
      String MIMEType = "text/html";
      String content = "<HTML><BODY>This is test results page from RawHTTPServer.</BODY></HTML>";
      String header = "HTTP 1.0 200 OK\r\n" +
                      "Server: RawHTTPServer\r\n" +
                      "Content-length: " + this.content.getBytes().length + "\r\n" +
                      "Content-type: " + MIMEType + "\r\n\rn\n";


      Client(Socket socket)
            throws IOException
      {
         super("RawHTTPServer-Client [" + (++clientCount) + "]");
         setDaemon(true);
         this.socket = socket;
         this.input = new BufferedInputStream(socket.getInputStream());
         this.output = new BufferedOutputStream(socket.getOutputStream());
         clients.add(this);
      }

      public void run()
      {
         while(started)
         {
            try
            {
               StringBuffer request = new StringBuffer(80);
               while(true)
               {
                  int c = input.read();
                  if(c == '\r' || c == '\n' || c == -1)
                  {
                     break;
                  }
                  request.append((char) c);
               }
               if(request.toString().indexOf("HTTP/") != -1)
               {
                  output.write(this.header.getBytes());
               }
               output.write(this.content.getBytes());
               output.flush();
            }
            catch(IOException e)
            {
               e.printStackTrace();
            }
            finally
            {
               if(socket != null)
               {
                  try
                  {
                     socket.close();
                  }
                  catch(IOException e)
                  {
                     e.printStackTrace();  //TODO: -TME Implement
                  }
               }
            }

/*
            try
            {
               int n = 0;
               byte[] buffer = new byte[1024];
               while (n != -1 || input.available() > 0)
               {
                  n = input.read(buffer);
                  System.out.println(new String(buffer));
               }
            }
            catch (IOException e)
            {
               e.printStackTrace();
            }
*/
//            try
//            {
//               output.write(new StringBuffer().append(200).toString().getBytes());
//               output.flush();
//            }
//            catch (IOException e)
//            {
//               e.printStackTrace();  //TODO: -TME Implement
//            }

         }
         clients.remove(this);
      }
   }

   private final class Listener extends Thread
   {
      public Listener()
      {
         super("RawHTTPServer-Listener");
         //setDaemon(true);
         setDaemon(false);
      }

      public void run()
      {
         while(started)
         {
            try
            {
               // blocks until a new client arrives
               Socket client = socket.accept();
               if(client != null)
               {
                  // make this a thread pool task
                  new Client(client).start();
               }
            }
            catch(Exception ex)
            {
               if(started)
               {
                  ex.printStackTrace();
               }
            }
         }
      }
   }


   public static void main(String[] args)
   {
      int port = RawHTTPServer.DEFAULT_PORT;
      if(args != null && args.length > 0)
      {
         port = Integer.parseInt(args[0]);
      }
      RawHTTPServer server = new RawHTTPServer(port);
      server.start();
   }

}