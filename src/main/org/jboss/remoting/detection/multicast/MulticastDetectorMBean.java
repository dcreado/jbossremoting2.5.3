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

import java.net.InetAddress;
import org.jboss.remoting.detection.AbstractDetectorMBean;


/**
 * MulticastDetectorMBean
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @version $Revision: 4968 $
 */
public interface MulticastDetectorMBean extends AbstractDetectorMBean
{
   /**
    * return the multicast address of the detector
    *
    * @return
    */
   public InetAddress getAddress();

   /**
    * set the interface address of the multicast
    *
    * @param ip
    */
   public void setAddress(InetAddress ip);

   /**
    * return the bind address of the detector
    *
    * @return
    */
   public InetAddress getBindAddress();

   /**
    * set the bind address
    *
    * @param ip
    */
   public void setBindAddress(InetAddress ip);

   /**
    * set the port for detections to be multicast to
    *
    * @param port
    */
   public void setPort(int port);

   /**
    * get the port that the detector is multicasting to
    *
    * @return
    */
   public int getPort();

   /**
    * @return The IP that is used to broadcast detection messages on via multicast.
    */
   String getDefaultIP();

   /**
    * @param defaultIP The IP that is used to broadcast detection messages on via multicast.
    */
   void setDefaultIP(String defaultIP);
   
   /**
    * @return The size of the byte array in the DatagramPacket.
    */
   int getBufferSize();
   
   /**
    * @param bufferSize The size of the byte array in the DatagramPacket.
    */
   void setBufferSize(int bufferSize);

}
