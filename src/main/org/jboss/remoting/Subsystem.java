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

package org.jboss.remoting;

/**
 * Predefined subsystem types.  These are strings since you could support a proprietary
 * subsystem or new specification by just adding the new subsystem string name on the server/client.
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @version $Revision: 566 $
 */
public interface Subsystem
{
   // special subsystem defined as self, which is the transport layer - used when the transport
   // wants to send messages to itself, such as ping, synching data on both sides, etc. which
   // don't actually get propograted up the transport layer
   public static final String SELF = "self";

   public static final String JMX = "jmx";
   public static final String JMS = "jms";
   public static final String EJB = "ejb";
   public static final String RMI = "rmi";
}
