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
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.net.SocketFactory;
import org.jboss.remoting.security.SSLSocketBuilder;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class SSLSimpleClient
{
   public static void main(String[] args) throws Exception
   {
      SSLSocketBuilder server = new SSLSocketBuilder();
//      server.setUseSSLSocketFactory(false);
//      server.setTrustStoreURL(".truststore");
//      server.setSecureSocketProtocol("SSL");

      SocketFactory sf = server.createSSLSocketFactory();

      Socket s = sf.createSocket(args[0], Integer.parseInt(args[1]));

      BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
      PrintWriter pw = new PrintWriter(s.getOutputStream());
      System.out.println("What time is it?");
      pw.println("What time is it?");
      pw.flush();
      System.out.println(br.readLine());
      s.close();
   }
}
