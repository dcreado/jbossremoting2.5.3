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

package org.jboss.remoting.stream;

import java.io.Serializable;

/**
 * The StreamCallPayload is used when making calls from the server
 * to the client to read from the original input stream.
 * It will contain the method name being called from the server side (i.e. available(), read(), etc.)
 * along with any parameters for the respective method.
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class StreamCallPayload implements Serializable
{
   static final long serialVersionUID = 3243358524048714566L;

   private String method = null;
   private Object[] paramArray = null;

   /**
    * Constructor which requires the name of the method to call on the
    * the original stream.
    *
    * @param methodCallName
    */
   public StreamCallPayload(String methodCallName)
   {
      this.method = methodCallName;
   }

   /**
    * Gets the method to call on the original stream.
    *
    * @return
    */
   public String getMethod()
   {
      return method;
   }

   /**
    * Sets the params for the method to call on the
    * stream.  For example, the Integer for markSupported(int supported).
    *
    * @param params
    */
   public void setParams(Object[] params)
   {
      this.paramArray = params;
   }

   /**
    * Gets the params for the method to call on the stream.
    *
    * @return
    */
   public Object[] getParams()
   {
      return this.paramArray;
   }
}