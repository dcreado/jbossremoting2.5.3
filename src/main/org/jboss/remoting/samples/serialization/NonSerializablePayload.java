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
package org.jboss.remoting.samples.serialization;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class NonSerializablePayload
{
   private String name = null;
   private int id = -1;

   /**
    * Is important that if using JBoss Serialization and non-serializable
    * payload objects, must have a void parameter contructor.  It can
    * be a private method that is not used externally, but is needed
    * by JBoss Serialization when reconstructing the object instance
    * when sent over the network.
    */
   private NonSerializablePayload()
   {
   }

   public NonSerializablePayload(String name, int id)
   {
      this.name = name;
      this.id = id;
   }

   public String getName()
   {
      return name;
   }

   public int getId()
   {
      return id;
   }

   public String toString()
   {
      return "NonSerializablePayload - name: " + name + ", id: " + id;
   }
}