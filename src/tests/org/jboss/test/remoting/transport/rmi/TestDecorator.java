
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
package org.jboss.test.remoting.transport.rmi;

import java.io.IOException;
import java.io.OutputStream;

import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.MarshallerDecorator;


/**
 * Part of unit test for JBREM-167.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Mar 12, 2008
 * </p>
 */
public class TestDecorator implements MarshallerDecorator, Marshaller
{
   /** The serialVersionUID */
   private static final long serialVersionUID = -3285464065838923918L;

   public Object addDecoration(Object obj) throws IOException
   {
      if (obj instanceof InvocationRequest)
      {
         InvocationRequest ir = (InvocationRequest) obj;
         if (ir.getParameter() instanceof Integer)
         {
            int i = ((Integer) ir.getParameter()).intValue();
            ir.setParameter(new Integer(i + 1));
         }
      }

      return obj;
   }

   public Marshaller cloneMarshaller() throws CloneNotSupportedException
   {
      return new TestDecorator();
   }

   public void write(Object dataObject, OutputStream output) throws IOException
   {
      // no-op
   }
}

