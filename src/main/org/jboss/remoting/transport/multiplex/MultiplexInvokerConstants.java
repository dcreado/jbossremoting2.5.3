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

/*
 * Created on Jan 23, 2006
 */
package org.jboss.remoting.transport.multiplex;


/**
 * Holds some constants for Multiplex system.  Now superceded by the <code>Multiplex</code>
 * class and deprecated.  Will be eliminated in future.
 * 
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * @version $Revision: 1248 $
 * <p>
 * Copyright (c) 2005
 * </p>
 * @deprecated
 */

public interface MultiplexInvokerConstants
{

   public static final String SERVER_MULTIPLEX_ID_KEY    = "serverMultiplexId";
   public static final String MULTIPLEX_CONNECT_PORT_KEY = "multiplexConnectPort";
   public static final String CLIENT_MULTIPLEX_ID_KEY    = "clientMultiplexId";
   public static final String MULTIPLEX_BIND_HOST_KEY 	 = "multiplexBindHost";
   public static final String MULTIPLEX_BIND_PORT_KEY	 = "multiplexBindPort";
   public static final String MULTIPLEX_CONNECT_HOST_KEY = "multiplexConnectHost";

}

