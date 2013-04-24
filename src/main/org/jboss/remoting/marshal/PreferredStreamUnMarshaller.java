/*
* JBoss, Home of Professional Open Source
* Copyright 2006, JBoss Inc., and individual contributors as indicated
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

/*
 * Created on Jan 26, 2007
 */
package org.jboss.remoting.marshal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;


/**
 * A PreferedStreamUnMarshaller can create from a raw InputStream the
 * particular InputStream it prefers to use

 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2000 $
 * <p>
 * Copyright (c) Jan 26, 2007
 * </p>
 */

public interface PreferredStreamUnMarshaller extends SerialUnMarshaller
{
   /**
    * An application that calls getMarshallingStream() should provide a
    * basic InpputStream, e.g., SocketIntputStream, which can be wrapped
    * to provide the facilities desired by the PreferredStreamUnMarshaller. 
    * 
    * @param inputStream a raw IntputStream
    * @return the InputStream to be used for marshalling
    * @throws IOException if unable to create InputStream
    */
   InputStream getMarshallingStream(InputStream inputStream) throws IOException;
   
   /**
    * An application that calls getMarshallingStream() should provide a
    * basic InpputStream, e.g., SocketIntputStream, which can be wrapped
    * to provide the facilities desired by the PreferredStreamUnMarshaller. 
    * 
    * @param inputStream a raw IntputStream
    * @param config a Map with configuration information (e.g., serialization type)
    * @return the InputStream to be used for marshalling
    * @throws IOException if unable to create InputStream
    */
   InputStream getMarshallingStream(InputStream inputStream, Map config) throws IOException;
}