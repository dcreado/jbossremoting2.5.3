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

package org.jboss.remoting.transport.web;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class WebUtil
{
   // list of content types
   public static String HTML = "text/html";
   public static String PLAIN = "text/plain";
   public static String SOAP = "application/soap+xml";
   public static String BINARY = "application/octet-stream";


   public static boolean isBinary(String requestContentType)
   {
      if(BINARY.equalsIgnoreCase(requestContentType))
      {
         return true;
      }
      else
      {
         return false;
      }
   }

   public static String getContentType(Object param)
   {
      if(param instanceof String)
      {
         return HTML;
      }
      else
      {
         return BINARY;
      }
   }


}