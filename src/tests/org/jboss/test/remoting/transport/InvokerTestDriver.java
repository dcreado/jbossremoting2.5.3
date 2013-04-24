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

package org.jboss.test.remoting.transport;

import org.jboss.jrunit.harness.TestDriver;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public abstract class InvokerTestDriver extends TestDriver
{
   public static final String REMOTING_METADATA = "remoting.metadata";
   public static final String JVM_MAX_HEAP_SIZE = "jvm.mx";

   protected String getClientJVMArguments()
   {
      return getJVMArguments();
   }

   /**
    * Returns the VM arguments to be passed to the vm when creating the server test cases (actually their harness).
    * The default value is null.
    *
    * @return
    */
   protected String getServerJVMArguments()
   {
      return getJVMArguments();
   }

   /**
    * Returns the VM arguments to be passed to the vm when creating the client and server test cases (actually their harness).
    * The default value is null.
    *
    * @return
    */
   protected String getJVMArguments()
   {
      String vmArgs = "";

      String metadata = System.getProperty(REMOTING_METADATA);
      if(metadata != null && metadata.length() > 0)
      {
         vmArgs = "-D" + REMOTING_METADATA + "=" + metadata;
      }
      String jvmMx = System.getProperty(JVM_MAX_HEAP_SIZE);
      if(jvmMx != null && jvmMx.length() > 0)
      {
         vmArgs = vmArgs + " -Xmx" + jvmMx + "m";
      }

      return vmArgs;
   }

}