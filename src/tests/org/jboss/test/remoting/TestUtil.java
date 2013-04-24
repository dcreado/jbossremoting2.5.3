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

package org.jboss.test.remoting;

import java.io.IOException;
import java.net.ServerSocket;
import java.security.SecureRandom;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class TestUtil
{
   private static final Logger log = Logger.getLogger(TestUtil.class);

   public static int getRandomPort()
   {
      Integer port = null;
      while(port == null)
      {
         port = getFreePort();
         if(port != null)
         {
            // validate port again, just in case two instances start on the port at same time.
            port = validatePort(port.intValue());
         }
      }
      return port.intValue();
   }

   private static Integer getFreePort()
   {

      Object o = new Object();
      String os = o.toString();
      os = os.substring(17);
      int n = Integer.parseInt(os, 16);
      int p = Math.abs(new SecureRandom(String.valueOf(System.currentTimeMillis() + n).getBytes()).nextInt(2000)) + 2000;

      return validatePort(p);
   }

   private static Integer validatePort(int p)
   {
      Integer port = null;
      ServerSocket socket = null;
      try
      {
         socket = new ServerSocket(p);
         port = new Integer(p);
      }
      catch(IOException e)
      {
         log.debug("port " + p + " already in use.  Will try another.");
         port = null;
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

            }
         }
      }
      return port;
   }
}