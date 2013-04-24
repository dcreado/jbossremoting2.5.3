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

import org.jboss.remoting.InvokerLocator;

import java.io.Serializable;

/**
 * This is the meta data for a server invoker that is contained within
 * detection messages.
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class ServerInvokerMetadata implements Serializable
{
   static final long serialVersionUID = 6971867047161027399L;

   private InvokerLocator locator;
   private String[] subSystems;

   public ServerInvokerMetadata(InvokerLocator locator, String[] supportedSubsystems)
   {
      this.locator = locator;
      this.subSystems = supportedSubsystems;
   }

   public InvokerLocator getInvokerLocator()
   {
      return locator;
   }

   public String[] getSubSystems()
   {
      return subSystems;
   }

   public String toString()
   {
      String subSystemStrings = "";
      if(subSystems != null)
      {
         for(int x = 0; x < subSystems.length; x++)
         {
            subSystemStrings = subSystemStrings + subSystems[x] + ",";
         }
         subSystemStrings = subSystemStrings.substring(0, subSystemStrings.length());
      }

      return "ServerInvokerMetadata:\n" +
             "locator: " + locator +
             "\nsubsystems: " + subSystemStrings;
   }
}