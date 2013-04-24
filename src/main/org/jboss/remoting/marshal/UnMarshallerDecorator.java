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

/**
 * On RMI invocations we can't use customer Marshallers, but we need to decorate Invocations (or any other objects)
 * with some data before sending the object.
 * <p/>
 * We could use a ByteArrayOutputStream and ByteArrayInputStream but that would be an expensive operation,
 * so we decided to keep the needed decoration on an interface. Marshaller implementations could refactor their logic to
 * the methods defined on this interface so we can have a cheaper marshalling with RMI.
 * $Id: UnMarshallerDecorator.java 566 2005-12-30 05:26:51Z telrod $
 *
 * @author <a href="mailto:tclebert.suconic@jboss.com">Clebert Suconic</a>
 */
public interface UnMarshallerDecorator
{
   public Object removeDecoration(Object obj) throws IOException;
}
