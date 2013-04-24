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
package org.jboss.remoting.detection.multicast;

import org.jboss.remoting.detection.AbstractDetector;
import org.jboss.remoting.detection.Detection;
import org.jboss.remoting.util.SecurityUtility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * MulticastDetector is a remoting detector that broadcasts detection messages using
 * muliticast.  The default multicast ip is 224.1.9.1 and port 2410.
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @author <a href="mailto:adrian.brock@happeningtimes.com">Adrian Brock</a>
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 * @version $Revision: 5000 $
 */
public class MulticastDetector extends AbstractDetector implements MulticastDetectorMBean
{
   private static int threadCounter = 0;

   private String defaultIP = "224.1.9.1";

   private InetAddress addr;
   private InetAddress bindAddr;
   private int port = 2410;
   private MulticastSocket socket;
   private Listener listener = new Listener("Remoting Multicast Detector - Listener Thread: " + threadCounter++);
   private int bufferSize = 10000;


   /**
    * @return The IP that is used to broadcast detection messages on via multicast.
    */
   public String getDefaultIP()
   {
      return defaultIP;
   }

   /**
    * @param defaultIP The IP that is used to broadcast detection messages on via multicast.
    */
   public void setDefaultIP(String defaultIP)
   {
      this.defaultIP = defaultIP;
   }

   /**
    * return the multicast address of the detector
    *
    * @return
    */
   public InetAddress getAddress()
   {
      return addr;
   }

   /**
    * set the interface address of the multicast
    *
    * @param ip
    */
   public void setAddress(InetAddress ip)
   {
      this.addr = ip;
   }

   /**
    * return the bind address of the detector
    *
    * @return
    */
   public InetAddress getBindAddress()
   {
      return bindAddr;
   }

   /**
    * set the bind address of the multicast
    *
    * @param ip
    */
   public void setBindAddress(InetAddress ip)
   {
      this.bindAddr = ip;
   }

   /**
    * get the port that the detector is multicasting to
    *
    * @return
    */
   public int getPort()
   {
      return port;
   }

   /**
    * set the port for detections to be multicast to
    *
    * @param port
    */
   public void setPort(int port)
   {
      this.port = port;
   }


   public int getBufferSize()
   {
      return bufferSize;
   }

   public void setBufferSize(int bufferSize)
   {
      this.bufferSize = bufferSize;
   }
   
   /**
    * called by MBeanServer to start the mbean lifecycle
    *
    * @throws Exception
    */
   public void start() throws Exception
   {
      if(addr == null)
      {
         addr = getAddressByName(defaultIP);
      }
      
      // check to see if we're running on a machine with loopback and no NIC
      InetAddress localHost = getLocalHost();
      if(bindAddr == null && localHost.getHostAddress().equals("127.0.0.1"))
      {
         // use this to bind so multicast will work w/o network
         this.bindAddr = localHost;
      }

      try
      {
         final SocketAddress saddr = new InetSocketAddress(bindAddr, port);
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IOException
            {
               socket = new MulticastSocket(saddr);
               socket.joinGroup(addr);
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (IOException) e.getCause();
      }

      super.start();

      if(listener == null)
      {
         listener = new Listener("Remoting Multicast Detector - Listener Thread: " + threadCounter++);
      }
      listener.start();
   }

   /**
    * called by the MBeanServer to stop the mbean lifecycle
    *
    * @throws Exception
    */
   public void stop() throws Exception
   {
      super.stop();
      if(listener != null)
      {
         try
         {
            listener.running = false;
            listener.interrupt();
         }
         catch (Exception e)
         {
            {
               log.warn(this + " Error stopping multicast detector.  " + e.getMessage());
            }         }
         listener = null;
      }
      if(socket != null)
      {
         try
         {
            socket.leaveGroup(addr);
            socket.close();
         }
         catch (IOException e)
         {
            log.warn(this + " Error stopping multicast detector.  " + e.getMessage());
         }
         socket = null;
      }
   }

   /**
    * subclasses must implement to provide the specific heartbeat protocol
    * for this server to send out to other servers on the network
    */
   protected void heartbeat()
   {
      if(socket != null)
      {
         Detection msg = createDetection();
         if (msg == null)
            return;
         
         try
         {
            if(log.isTraceEnabled())
            {
               log.trace(this + " sending heartbeat: " + msg);
            }
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutputStream objectOut = new ObjectOutputStream(byteOut);
            objectOut.writeObject(msg);
            objectOut.flush();
            byteOut.flush();
            byte buf[] = byteOut.toByteArray();
            DatagramPacket p = new DatagramPacket(buf, buf.length, addr, port);
            socket.send(p);
         }
         catch(Throwable ex)
         {
            // its failed
            log.debug(this + " heartbeat failed", ex);
         }
      }
   }

   protected void forceHeartbeat()
   {
      if(socket != null)
      {
         String msg = "Send heartbeat";
         try
         {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutputStream objectOut = new ObjectOutputStream(byteOut);
            objectOut.writeObject(msg);
            objectOut.flush();
            byteOut.flush();
            byte buf[] = byteOut.toByteArray();
            DatagramPacket p = new DatagramPacket(buf, buf.length, addr, port);
            socket.send(p);

            /**
             * This is a bit of a hack, but am going to wait a few seconds to
             * allow for any group members (other multicast detectors) to receive
             * the msg and then respond themselves with their detection messages.
             * Since don't know number of servers that are members of the group,
             * can't really wait until all detection messages, just hope they come
             * in before end of waiting.
             */
            Thread.currentThread().sleep(2000);

         }
         catch(Throwable ex)
         {
            // its failed
            log.debug(this + " forced heartbeat failed", ex);
         }
      }
   }

   private void listen(DatagramPacket p, byte[] buf)
   {
      if(socket != null)
      {
         try
         {
            // should block until we get a multicast
            socket.receive(p);

            // take the multicast, and deserialize into the detection event
            ByteArrayInputStream byteInput = new ByteArrayInputStream(buf);
            ObjectInputStream objectInput = new ObjectInputStream(byteInput);
            Object obj = objectInput.readObject();
            if(obj instanceof Detection)
            {
               Detection msg = (Detection)obj;
               if(log.isTraceEnabled())
               {
                  log.trace(this + " received detection: " + msg);
               }

               // let the subclass do the hard work off handling detection
               detect(msg);
            }
            else
            {
               // for now, assume anything *not* of type Detection
               // is a prompt to send out detection msg
               heartbeat();
            }
         }
         catch(Throwable e)
         {
            if(e instanceof java.io.InvalidClassException)
            {
               return;
            }
            if(socket != null)
            {
               log.debug(this + " Error receiving detection", e);
            }
         }
      }
   }

   private final class Listener extends Thread
   {
      boolean running = true;

      public Listener(String name)
      {
         super(name);
      }

      public void run()
      {
         log.debug("using bufferSize: " + bufferSize);
         byte[] buf = new byte[bufferSize];
         DatagramPacket p = new DatagramPacket(buf, 0, buf.length);
         //p.setAddress(addr);
         //p.setPort(port);
         while(running)
         {
            listen(p, buf);
         }
      }
   }
   
   static private InetAddress getLocalHost() throws UnknownHostException
   {
      if (SecurityUtility.skipAccessControl())
      {
         try
         {
            return InetAddress.getLocalHost();
         }
         catch (IOException e)
         {
            return InetAddress.getByName("127.0.0.1");
         }
      }

      try
      {
         return (InetAddress) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IOException
            {
               try
               {
                  return InetAddress.getLocalHost();
               }
               catch (IOException e)
               {
                  return InetAddress.getByName("127.0.0.1");
               }
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (UnknownHostException) e.getCause();
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
