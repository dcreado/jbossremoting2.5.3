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
package org.jboss.remoting.callback;

import org.jboss.remoting.SerializableStore;


/**
 * The MBean interface to the CallbackStore implementation.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public interface CallbackStoreMBean extends SerializableStore
{
   /**
    * Gets the file path for the directory where the objects will be stored.
    *
    * @return
    */
   String getStoreFilePath();

   /**
    * Sets teh file path for the directory where the objects will be stored.
    *
    * @param filePath
    */
   void setStoreFilePath(String filePath);

   /**
    * Gets the file suffix for each of the files that objects will be persisted to.
    *
    * @return
    */
   String getStoreFileSuffix();

   /**
    * Sets the file suffix for each of the files that objects will be persisted to.
    *
    * @param fileSuffix
    */
   void setStoreFileSuffix(String fileSuffix);

}
