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
 * SubsystemNotSupported is raised by a ServerInvoker when the target subsystem is not
 * supported on the target VM.
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @version $Revision: 1216 $
 */
public class SubsystemNotSupported extends Exception
{
   private String subsystem;
   private InvokerLocator locator;
   private static final long serialVersionUID = 7211573237413019835L;

   public SubsystemNotSupported(String subsystem, InvokerLocator locator)
   {
      super("Subsystem '" + subsystem + "' not supported on target VM (" + locator + ")");
      this.subsystem = subsystem;
      this.locator = locator;
   }

   public String getSubsystem()
   {
      return subsystem;
   }

   public InvokerLocator getLocator()
   {
      return locator;
   }
}
