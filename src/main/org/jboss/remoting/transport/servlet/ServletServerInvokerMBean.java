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
package org.jboss.remoting.transport.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The MBean interface for the ServletServerInvoker.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public interface ServletServerInvokerMBean
{
   /**
    * Method to be called by the servlet to handle the incoming request.
    *
    * @param request
    * @param response
    * @throws ServletException
    * @throws IOException
    */
   void processRequest(HttpServletRequest request, HttpServletResponse response)
         throws ServletException, IOException;

   public byte[] processRequest(HttpServletRequest request,
                                byte[] requestByte,
                                HttpServletResponse response)
         throws ServletException, IOException;

}
