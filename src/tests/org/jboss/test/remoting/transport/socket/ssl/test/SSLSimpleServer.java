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
package org.jboss.test.remoting.transport.socket.ssl.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Date;
import javax.net.ServerSocketFactory;
import org.jboss.remoting.security.SSLSocketBuilder;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class SSLSimpleServer
{
   private ServerSocketFactory serverSocketFactory;
   private boolean keepRunning = true;


   public void startServer()
         throws IOException, NoSuchAlgorithmException, KeyManagementException,
                CertificateException, UnrecoverableKeyException, KeyStoreException
   {
      String keyStoreFilePath = this.getClass().getResource("../.keystore").getFile();
      System.setProperty("javax.net.ssl.keyStore", keyStoreFilePath);
      System.setProperty("javax.net.ssl.keyStorePassword", "unit-tests-server");


      if(serverSocketFactory == null)
      {
         SSLSocketBuilder server = new SSLSocketBuilder();
         //server.setUseSSLServerSocketFactory(false);
//         server.setKeyStoreURL(".keystore");
//         server.setKeyStorePassword("unit-tests-server");
//         /*
//          * This is optional since if not set, will use
//          * the key store password (and are the same in this case)
//          */
//         //server.setKeyPassword("unit-tests-server");
//
//         server.setSecureSocketProtocol("SSL");

         serverSocketFactory = server.createSSLServerSocketFactory();
      }
      InetAddress addr = InetAddress.getLocalHost();
      final ServerSocket ss = serverSocketFactory.createServerSocket(9097, 200, addr);

      while(keepRunning)
      {
         final Socket sock = ss.accept();
         new Thread()
         {
            public void run()
            {
               try
               {
                  BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                  PrintWriter pw = new PrintWriter(sock.getOutputStream());

                  String data = br.readLine();
                  pw.println("The time is: " + new Date());
                  pw.close();
                  sock.close();
               }
               catch(IOException e)
               {
                  e.printStackTrace();
               }

            }
         }.start();
      }
   }

   public void stopServer()
   {
      keepRunning = false;
   }

   /**
    * Allows for the setting of the server socket factory to use.  This will bypass the creating of any
    * custom server socket factory in favor of the one passed.
    *
    * @param ssf
    */
   public void setServerSocketFactory(ServerSocketFactory ssf)
   {
      this.serverSocketFactory = ssf;
   }


   public static void main(String[] args) throws Exception
   {
      try
      {
         SSLSimpleServer server = new SSLSimpleServer();
         server.startServer();
      }
      catch(Throwable thr)
      {
         thr.printStackTrace();
      }
   }
}
