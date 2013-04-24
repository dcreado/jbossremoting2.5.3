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


package org.jboss.remoting.serialization;

import java.io.IOException;

/**
 * Empty implementation of a MarshalledValue, as {@link SerializationManager} is now managing creationg
 * of this object.
 * We need this class just as a hook for implementation on JBossAS
 * $Id: RemotingMarshalledValue.java 741 2006-02-27 20:53:50Z csuconic $
 *
 * @author <a href="mailto:tclebert.suconic@jboss.com">Clebert Suconic</a>
 */
public abstract class RemotingMarshalledValue implements IMarshalledValue
{

   /**
    * The object has to be unserialized only when the first get is executed.
    */
   public abstract Object get() throws IOException, ClassNotFoundException;
}
