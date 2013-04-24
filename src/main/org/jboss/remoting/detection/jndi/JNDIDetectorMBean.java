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
package org.jboss.remoting.detection.jndi;

import org.jboss.remoting.detection.AbstractDetectorMBean;


/**
 * JNDIDetectorMBean
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @version $Revision: 566 $
 */
public interface JNDIDetectorMBean extends AbstractDetectorMBean
{
   public int getPort();

   public void setPort(int port);

   public String getHost();

   public void setHost(String host);

   public String getContextFactory();

   public void setContextFactory(String contextFactory);

   public String getURLPackage();

   public void setURLPackage(String urlPackage);

   public int getCleanDetectionNumber();

   public void setCleanDetectionNumber(int cleanDetectionNumber);

}
