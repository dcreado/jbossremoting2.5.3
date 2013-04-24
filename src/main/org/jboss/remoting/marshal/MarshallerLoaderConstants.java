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

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public interface MarshallerLoaderConstants
{
   public final String LOAD_MARSHALLER_METHOD = "load_marshaller";
   public final String LOAD_UNMARSHALLER_METHOD = "load_unmarshaller";
   public final String GET_MARSHALLER_METHOD = "get_marshaller";
   public final String GET_UNMARSHALLER_METHOD = "get_unmarshaller";
   public final String LOAD_CLASS_METHOD = "load_class";
   public final String CLASSNAME = "classname";
}
