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
import java.io.OutputStream;
import java.io.Serializable;

/**
 * Interface that all data marshallers must implements.
 * Requires them to take Java data objects and convert
 * primitive java data types (i.e. byte[]) and write
 * to output provided.
 * <p/>
 * Since the Marshaller is only responsible for doing
 * the conversion to primitive type, does not make sense
 * that would be supplied any type of object output to
 * write to, as this implies that the object that it
 * writes would be converted to bytes at some other
 * point external to the marshaller.
 *
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public interface Marshaller extends Serializable
{
   /**
    * Marshaller will need to take the dataObject and convert
    * into primitive java data types and write to the
    * given output.
    *
    * @param dataObject Object to be writen to output
    * @param output     The data output to write the object
    *                   data to.
    */
   public void write(Object dataObject, OutputStream output) throws IOException;

   public Marshaller cloneMarshaller() throws CloneNotSupportedException;

}