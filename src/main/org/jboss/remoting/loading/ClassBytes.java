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
package org.jboss.remoting.loading;

/**
 * ClassBytes is a serialized object that represents a class name and the class bytes as a byte array.
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @version $Revision: 566 $
 */
public class ClassBytes implements java.io.Serializable
{
   static final long serialVersionUID = 9163990179051656161L;
   protected String className;
   protected byte classBytes[];

   public ClassBytes(String className, byte data[])
   {
      this.className = className;
      this.classBytes = data;
   }

   public String toString()
   {
      return "ClassBytes [class=" + className + ",value=" + classBytes + "]";
   }

   public String getClassName()
   {
      return className;
   }

   public byte[] getClassBytes()
   {
      return classBytes;
   }
}
