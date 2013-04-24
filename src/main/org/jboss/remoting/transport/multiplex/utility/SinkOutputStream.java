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

/*
 * Created on Jul 31, 2005
 */
 
package org.jboss.remoting.transport.multiplex.utility;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 
 * <p>
 * Copyright (c) 2005
 * <p>
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * 
 * @deprecated As of release 2.4.0 the multiplex transport will no longer be actively supported.
 */
public class SinkOutputStream extends OutputStream
{
   private static SinkOutputStream sinkOutputStream;

   private SinkOutputStream()
   {
   }

   
   public static SinkOutputStream getSinkOutputStream()
   {
      if (sinkOutputStream == null)
         sinkOutputStream = new SinkOutputStream();
      
      return sinkOutputStream;
   }

   public void write(int b) throws IOException
   {
   }
   
   public void close()
   {
   }

}
