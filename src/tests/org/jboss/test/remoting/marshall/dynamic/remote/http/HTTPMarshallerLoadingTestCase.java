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

package org.jboss.test.remoting.marshall.dynamic.remote.http;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.jboss.jrunit.harness.TestDriver;

/**
 * This should be used as the main test case for the invoker client/server.
 * It will start one instance of the client and one of the server and will
 * gather the test results and report them in standard JUnit format.  When
 * wanting to run JUnit test for invoker, this is the class to use.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class HTTPMarshallerLoadingTestCase extends TestDriver
{
   /**
    * Returns the classpath to be added to the classpath used to start the client tests.
    * Default return is null, which means no extra classpath will be added.
    *
    * @return
    */
   protected String getExtendedServerClasspath()
   {
      String path = null;
      try
      {
         path = (String)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IOException
            {
               return System.getProperty("loader.path");
            }
         });
      }
      catch (PrivilegedActionException e)
      {
      }

      return path;
   }

   public void declareTestClasses()
   {
      addTestClasses(MarshallerLoadingClient.class.getName(),
                     1,
                     MarshallerLoadingServer.class.getName());
   }
   
   protected String getClientJVMArguments()
   {
      return getJVMArguments();
   }

   protected String getServerJVMArguments()
   {
      return getJVMArguments();
   }
   
   protected String getJVMArguments()
   {
      String vmArgs = "";

      String metadata = System.getProperty("remoting.metadata");
      if(metadata != null && metadata.length() > 0)
      {
         vmArgs = "-Dremoting.metadata=" + metadata;
      }

      return vmArgs;
   }
}