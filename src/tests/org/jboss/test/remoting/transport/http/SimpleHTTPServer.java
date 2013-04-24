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
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class SimpleHTTPServer extends Thread
{
   private byte[] content;
   private byte[] header;
   private int port = 80;
   private static boolean timeout = false;

   public SimpleHTTPServer(String data, String encoding, String MIMEType, int port)
         throws UnsupportedEncodingException
   {
      this(data.getBytes(encoding), encoding, MIMEType, port);
   }

   public SimpleHTTPServer(byte[] data, String encoding, String MIMEType, int port)
         throws UnsupportedEncodingException
   {
      this.content = data;
      this.port = port;
      String header = "HTTP/1.1 200 OK\r\n" +
                      "Server: SimpleHTTPServer 1.0\r\n" +
                      "Content-length: " + this.content.length + "\r\n" +
                      "Content-type: " + MIMEType + "\r\n\r\n";
      this.header = header.getBytes("ASCII");
   }


   protected void startTimer(Thread currentThread)
   {
      Timer timer = new Timer(false);
      timer.schedule(new TimeoutTimerTask(currentThread), 3000);
   }

   public void run()
   {
      try
      {
         ServerSocket server = new ServerSocket(this.port);
         System.out.println("Accepting connections on port " + server.getLocalPort());
         System.out.println("Data to be sent: ");
         System.out.write(this.content);

         while(true)
         {
            Socket connection = null;
            try
            {
               connection = server.accept();
               OutputStream out = new BufferedOutputStream(connection.getOutputStream());
               InputStream in = new BufferedInputStream(connection.getInputStream());

               //startTimer(Thread.currentThread());

               StringBuffer request = new StringBuffer(80);
               try
               {
                  int c = in.read();
                  while(c != -1 && !timeout)
                  {
                     int q = 0;
                     if(request.length() > 0)
                     {
                        q = request.charAt(request.length() - 1);
                     }
                     request.append((char) c);
                     //System.out.println(request);
                     int n = in.read();

                     if(c == '\r' && n == '\n')
                     {
                        if(q == '\n')
                        {
                           break;
                        }
                        else
                        {
                           c = n;
                        }
                     }
                     else
                     {
                        c = n;
                     }
                     //Thread.sleep(50);
                  }
               }
               catch(Exception e)
               {
                  e.printStackTrace();
               }
               System.out.println("\n\nHTTP Request:\n\n" + request + "\n\n");

               StringBuffer requestHeader = new StringBuffer(80);
               for(int x = 0; x < request.length(); x++)
               {
                  char cr = request.charAt(x);
                  if(cr == '\r' || cr == '\n' || cr == -1)
                  {
                     break;
                  }
                  requestHeader.append(cr);
               }
               if(requestHeader.toString().indexOf("HTTP/") != -1)
               {
                  out.write(this.header);
               }
               out.write(this.content);
               out.flush();
            }
            catch(IOException e)
            {
            }
            finally
            {
               if(connection != null)
               {
                  connection.close();
               }
            }
         }
      }
      catch(IOException e)
      {
         System.err.println("Could not start server.  Port " + this.port + " occupied.");
      }

   }

   public class TimeoutTimerTask extends TimerTask
   {
      private Thread curThread;

      public TimeoutTimerTask(Thread current)
      {
         this.curThread = current;
      }

      public void run()
      {
         timeout = true;
         curThread.interrupt();
      }
   }

   public static void main(String[] args)
   {
      try
      {
         String contentType = "text/plain";
         if(args[0].endsWith(".html") || args[0].endsWith(".htm"))
         {
            contentType = "text/html";
         }

         InputStream in = new FileInputStream(args[0]);
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         int b;
         while((b = in.read()) != -1)
         {
            out.write(b);
         }
         byte[] data = out.toByteArray();

         int port;
         try
         {
            port = Integer.parseInt(args[1]);
            if(port < 1 || port > 65535)
            {
               port = 80;
            }
         }
         catch(Exception e)
         {
            port = 80;
         }

         String encoding = "ASCII";
         if(args.length >= 2)
         {
            encoding = args[2];
         }
         Thread t = new SimpleHTTPServer(data, encoding, contentType, port);
         t.start();
      }
      catch(ArrayIndexOutOfBoundsException e)
      {
         System.out.println("Usage: java SimpleHTTPServer filename port encoding");
      }
      catch(Exception e)
      {
         System.err.println(e);
      }
   }
}