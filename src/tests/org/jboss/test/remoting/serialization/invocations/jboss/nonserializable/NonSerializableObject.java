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
package org.jboss.test.remoting.serialization.invocations.jboss.nonserializable;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class NonSerializableObject
{
   private String name = null;
   private int number = -1;

   /**
    * To allow serialization over JBossSerialization
    */
   private NonSerializableObject()
   {
   }

   public NonSerializableObject(String name, int number)
   {
      this.name = name;
      this.number = number;
   }

   public String getName()
   {
      return name;
   }

   public int getNumber()
   {
      return number;
   }

   public String toString()
   {
      return "NonSerializableObject: name=" + name + ", number=" + number;
   }

   public boolean equals(Object obj)
   {
      if(obj instanceof NonSerializableObject)
      {
         NonSerializableObject otherObj = (NonSerializableObject) obj;
         if(name.equals(otherObj.getName()) && number == otherObj.getNumber())
         {
            return true;
         }
         else
         {
            return false;
         }
      }
      else
      {
         return false;
      }
   }
}
