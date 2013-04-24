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

import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import java.util.Random;

import org.jboss.logging.Logger;

/**
 * PortUtil is a set of utilities for dealing with TCP/IP ports
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 * @version $Revision: 5342 $
 */
public class PortUtil
{
   public static final String MIN_PORT = "minPort";
   public static final String MAX_PORT = "maxPort";
   
   private static final Logger log = Logger.getLogger(PortUtil.class);
   private static final int MIN_UNPRIVILEGED_PORT = 1024;
   private static final int MAX_LEGAL_PORT = 65535;

   private static int portCounter = 0;
   private static int retryMax = 50;
   
   private static int minPort = MIN_UNPRIVILEGED_PORT;
   private static int maxPort = MAX_LEGAL_PORT;

   static
   {
      portCounter = getRandomStartingPort();
   }

   /**
    * Checks to see if the specified port is free.
    *
    * @param p
    * @return true if available, false if already in use
    */
   public static boolean checkPort(final int p, final String host)
   {
      boolean available = true;
      ServerSocket socket = null;

      try
      {
         try
         {
            socket = (ServerSocket) AccessController.doPrivileged( new PrivilegedExceptionAction()
            {
               public Object run() throws Exception
               {
                  InetAddress inetAddress = InetAddress.getByName(host);
                  return new ServerSocket(p, 0, inetAddress);
               }
            });
         }
         catch (PrivilegedActionException e)
         {
            available = false;
            throw (IOException) e.getCause();
         }
      }
      catch (UnknownHostException e)
      {
         log.warn("unknown host: " + host);
      }
      catch (IOException e)
      {
         if ("Protocol family unavailable".equalsIgnoreCase(e.getMessage()) ||
             "Protocol family not supported".equalsIgnoreCase(e.getMessage()))
         {
            log.debug("perhaps IPv6 is not available: " + e.getMessage());
         }
         else
         {
            log.debug("port " + p + " already in use.  Will try another.", e.getCause());
         }
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
      
      return available;
   }

   /**
    * Will try to find a port that is not in use up to 50 tries, at which point,
    * will throw an exception.
    * @return
    */
   public static int findFreePort(String host) throws IOException
   {
      Integer port = null;
      int tryCount = 0;
      while(port == null && tryCount < retryMax)
      {
         port = getFreePort(host);
         if(port != null)
         {
            // validate port again, just in case two instances start on the port at same time.
            if(!checkPort(port.intValue(), host))
            {
               port = null;
            }
         }
         tryCount++;
      }
      if(tryCount >= retryMax)
      {
         throw new IOException("Can not find a free port for use.");
      }
      return port.intValue();
   }

   private static Integer getFreePort(String host)
   {
      int p = getNextPort();

      if(checkPort(p, host))
      {
         return new Integer(p);
      }
      else
      {
         return null;
      }
   }

   private static synchronized int getNextPort()
   {
	  if (portCounter < maxPort)
		  return portCounter++;
	  
	  portCounter = minPort;
	  return maxPort;
   }

   public static int getRandomStartingPort()
   {  
      int range = maxPort - minPort + 1;
      int port = new Random(System.currentTimeMillis()).nextInt(range) + minPort;
      return port;
   }
   
   public static synchronized int getMinPort()
   {
      return minPort;
   }

   public static synchronized void setMinPort(int minPort) throws IllegalStateException
   {
      if (minPort > PortUtil.maxPort)
      {
         String msg = "trying to set minPort to value greater than maxPort: " + minPort + " > " + PortUtil.maxPort;
         log.debug(msg);
         throw new IllegalStateException(msg);
      }
      if (minPort < PortUtil.minPort)
      {
         log.debug("will not set minPort to " + minPort + ": minPort is already " + PortUtil.minPort);
         return;
      }
      log.debug("setting minPort to " + minPort);
      PortUtil.minPort = minPort;
   }

   public static synchronized int getMaxPort()
   {
      return maxPort;
   }

   public static synchronized void setMaxPort(int maxPort)
   {
      if (maxPort < PortUtil.minPort)
      {
         String msg = "trying to set maxPort to value less than minPort: " + maxPort + " < " + PortUtil.minPort;
         log.debug(msg);
         throw new IllegalStateException(msg);
      }
      if (maxPort > PortUtil.maxPort)
      {
         log.debug("will not set maxPort to " + maxPort + ": maxPort is already " + PortUtil.maxPort);
         return;
      }
      log.debug("setting maxPort to " + maxPort);
      PortUtil.maxPort = maxPort;
   }
   
   public static synchronized void updateRange(Map config)
   {
      if (config != null)
      {
         int savedMinPort = getMinPort();
         Object o = config.get(MIN_PORT);
         if (o instanceof String)
         {
            try
            {
               setMinPort(Integer.parseInt((String) o));
            }
            catch (NumberFormatException  e)
            {
               log.error("minPort parameter has invalid format: " + o);
            }
         }
         else if (o != null)
         {
            log.error("minPort parameter must be a string in integer format: " + o);
         }
         
         int savedMaxPort = getMaxPort();
         o = config.get(MAX_PORT);
         if (o instanceof String)
         {
            try
            {
               setMaxPort(Integer.parseInt((String) o));
            }
            catch (NumberFormatException  e)
            {
               log.error("maxPort parameter has invalid format: " + o);
            }
         }
         else if (o != null)
         {
            log.error("maxPort parameter must be a string in integer format: " + o);
         }
         
         if (savedMinPort != getMinPort() || savedMaxPort != getMaxPort())
         {
            portCounter = getRandomStartingPort();
         }
      }
   }

   public static void main(String args[])
   {
      try
      {
         System.out.println("port - " + findFreePort("localhost"));
      }
      catch(Exception ex)
      {
         ex.printStackTrace();
      }
   }
}
