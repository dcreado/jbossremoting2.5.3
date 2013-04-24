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
package org.jboss.remoting.detection;

import org.w3c.dom.Element;

/**
 * This provides a MBean accessible interface for setting domain configuration
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public interface AbstractDetectorMBean extends Detector
{
   /**
    * set the configuration for the domains to be recognized by detector
    *
    * @param xml
    * @jmx.managed-attribute description="Domains is an xml element indicating domains to be recognized by detector"
    * access="read-write"
    */
   public void setConfiguration(Element xml) throws Exception;

   /**
    * The <code>getDomains</code> method
    *
    * @return an <code>Element</code> value
    * @jmx.managed-attribute
    */
   public Element getConfiguration();

   /**
    * The amount of time to wait between sending (and sometimes receiving) detection messages.
    *
    * @param heartbeatTimeDelay
    * @throws IllegalArgumentException
    */
   void setHeartbeatTimeDelay(long heartbeatTimeDelay);

   /**
    * The amount of time to wait between sending (and sometimes receiving) detection messages.
    *
    * @return
    */
   long getHeartbeatTimeDelay();

   /**
    * The amount of time which can elapse without receiving a detection event before a server
    * will be suspected as being dead and peroforming an explicit invocation on it to verify it is alive.
    *
    * @param defaultTimeDelay time in milliseconds
    * @throws IllegalArgumentException
    */
   void setDefaultTimeDelay(long defaultTimeDelay) throws IllegalArgumentException;

   /**
    * @return The amount of time which can elapse without receiving a detection event before a server
    *         will be suspected as being dead and peroforming an explicit invocation on it to verify it is alive.
    */
   long getDefaultTimeDelay();
}
