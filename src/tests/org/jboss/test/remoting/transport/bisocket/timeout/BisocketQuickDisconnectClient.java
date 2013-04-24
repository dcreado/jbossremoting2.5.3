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
package org.jboss.test.remoting.transport.bisocket.timeout;

import org.jboss.test.remoting.timeout.QuickDisconnectClientParent;


/** 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2744 $
 * <p>
 * Copyright Feb 17, 2007
 * </p>
 */
public class BisocketQuickDisconnectClient extends QuickDisconnectClientParent
{  
   protected String getTransport()
   {
      return "bisocket";
   }
   
   /**
    * It seems that in jdk 1.4 (at least), HttpURLConnection has a default timeout
    * of 1000 ms, so the tests are run with a shorter timeout.
    */
   protected int shortTimeout()
   {
      return 500;
   }
   
   protected String shortTimeoutString()
   {
      return "500";
   }
}
