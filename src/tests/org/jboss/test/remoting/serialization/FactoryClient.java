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

package org.jboss.test.remoting.serialization;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.Socket;
import org.jboss.remoting.serialization.SerializationStreamFactory;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public abstract class FactoryClient extends TestCase
{
   protected String address = "localhost";
   protected int port = 6700;

   private boolean enableTcpNoDelay = false;
   private int timeout = 60000;

   private Socket socket = null;

   public void setUp() throws Exception
   {
      socket = new Socket(address, port);
      socket.setSoTimeout(timeout);
      socket.setTcpNoDelay(enableTcpNoDelay);

   }

   public void tearDown() throws Exception
   {
      if(socket != null)
      {
         socket.close();
      }
   }

   public void testClientCall() throws Exception
   {
      configureStream();

      ObjectOutput output = SerializationStreamFactory.getManagerInstance().createOutput(socket.getOutputStream());
      ObjectInput input = SerializationStreamFactory.getManagerInstance().createRegularInput(socket.getInputStream());

      output.writeObject("This is the request");
      output.flush();

      Object obj = input.readObject();

      System.out.println("response: " + obj);

      assertEquals("Response value is not what was expected.", FactoryServer.RESPONSE, obj);

   }

   protected abstract void configureStream() throws Exception;
}