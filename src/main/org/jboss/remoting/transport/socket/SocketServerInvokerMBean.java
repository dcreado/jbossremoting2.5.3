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

package org.jboss.remoting.transport.socket;

import org.jboss.remoting.ServerInvokerMBean;

/**
 * MBean interface.
 */
public interface SocketServerInvokerMBean extends ServerInvokerMBean
{

   /**
    * Starts the invoker.
    */
   void start() throws java.io.IOException;

   /**
    * Stops the invoker.
    */
   void stop();

   int getCurrentThreadPoolSize();

   int getCurrentClientPoolSize();

   /**
    * Getter for property numAcceptThreads
    *
    * @return The number of threads that exist for accepting client connections
    */
   int getNumAcceptThreads();

   /**
    * Setter for property numAcceptThreads
    *
    * @param size The number of threads that exist for accepting client connections
    */
   void setNumAcceptThreads(int size);

   /**
    * Setter for max pool size. The number of server threads for processing client. The default is 300.
    *
    * @return
    */
   int getMaxPoolSize();

   /**
    * The number of server threads for processing client. The default is 300.
    *
    * @param maxPoolSize
    */
   void setMaxPoolSize(int maxPoolSize);

   /**
    * Getter for property serverBindPort.
    *
    * @return Value of property serverBindPort.
    */
   int getServerBindPort();

   int getBacklog();

   void setBacklog(int backlog);

}
