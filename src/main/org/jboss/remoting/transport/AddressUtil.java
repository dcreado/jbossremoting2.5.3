
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
package org.jboss.remoting.transport;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.jboss.logging.Logger;
import org.jboss.remoting.util.SecurityUtility;



/**
 * Checks if an IP interface address is usable.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Jan 30, 2008
 * </p>
 */
public class AddressUtil
{
   private static Logger log = Logger.getLogger(AddressUtil.class);
   
   public static boolean checkAddress(String host) throws Exception
   {
      return checkAddress(host, 5000);
   }
   
   public static boolean checkAddress(final String host, int timeout) throws Exception
   {
      try
      {
         log.trace("checking host: " + host);
         int port = PortUtil.findFreePort(host);
         InetAddress addr = getAddressByName(host);
         ServerTestThread t1 = new ServerTestThread(addr, port);
         t1.setDaemon(true);
         t1.start();
         ClientTestThread t2 = new ClientTestThread(addr, port);
         t2.setDaemon(true);
         t2.start();
         t2.join(timeout);
         return t2.ok;
      }
      catch (Exception e)
      {
         return false;
      }
   }
   
   static class ServerTestThread extends Thread
   {
      InetAddress addr;
      int port; 
      
      ServerTestThread(InetAddress addr, int port)
      {
         this.addr = addr;
         this.port = port;
      }
      
      public void run()
      {
         try
         {
            AccessController.doPrivileged( new PrivilegedExceptionAction()
            {
               public Object run() throws Exception
               {
                  ServerSocket ss = new ServerSocket(port, 0, addr);
                  Socket s = ss.accept();
                  s.close();
                  ss.close();
                  log.trace("ServerTestThread ok: " + addr + ":" + port);
                  return null;
               }
            });
         }
         catch (PrivilegedActionException e)
         {
            log.trace("error in ServerTestThread", e);
         }
      }
   }
   
   static class ClientTestThread extends Thread
   {
      public boolean ok;
      
      InetAddress addr;
      int port; 
      
      ClientTestThread(InetAddress addr, int port)
      {
         this.addr = addr;
         this.port = port;
      }
      
      public void run()
      {
         try
         {
            Socket s = new Socket(addr, port);
            s.close();
            ok = true;
            log.trace("ClientTestThread ok: " + addr + ":" + port);
         }
         catch (Exception e)
         {
            log.trace("error in ClientTestThread", e);
         }
      }
   }
   
   static private InetAddress getAddressByName(final String host) throws UnknownHostException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return InetAddress.getByName(host);
      }
      
      try
      {
         return (InetAddress)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IOException
            {
               return InetAddress.getByName(host);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (UnknownHostException) e.getCause();
      }
   }
}

