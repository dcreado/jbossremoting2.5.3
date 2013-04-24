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

package org.jboss.remoting.marshal;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;

/**
 * Takes a marshalled byte array and converts to a Java data object.
 *
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public interface UnMarshaller extends Serializable
{
   /**
    * Will read from the inputstream and converty contents to java Object.
    *
    * @param inputStream stream to read data from to do conversion.
    * @param metadata can be any transport specific metadata (such as headers from http transport).
    *        This can be null, depending on if transport supports metadata.
    *
    * @return
    * @throws IOException all specific i/o exceptions need to be thrown as this.
    * @throws ClassNotFoundException will be thrown if during the unmarshalling process can not find
    *         a specific class within classloader.
    */
   Object read(InputStream inputStream, Map metadata) throws IOException, ClassNotFoundException;

   /**
    * Set the class loader to use for unmarhsalling. This may be needed when need to have access to
    * class definitions that are not part of this unmarshaller's parent classloader (especially
    * when doing remote classloading).
    */
   void setClassLoader(ClassLoader classloader);

   UnMarshaller cloneUnMarshaller() throws CloneNotSupportedException;

}