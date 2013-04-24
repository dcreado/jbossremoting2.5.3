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

package org.jboss.test.remoting.performance.raw.rmi;

import org.jboss.test.remoting.performance.synchronous.PerformanceServerTest;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class RMIPerformanceServer extends PerformanceServerTest
{
   private int rmiPort = 1099;
   private Registry registry = null;

   public void init(Map metatdata) throws Exception
   {
      //super.init(metatdata);

      try
      {
         registry = LocateRegistry.createRegistry(rmiPort);


         RMIServerRemote engine = new RMIServer(host);
//         Naming.rebind(name, engine);
         registry.rebind("//" + host + "/RMIServer", engine);
      }
      catch(RemoteException e)
      {
         e.printStackTrace();
      }

   }

   public void tearDown() throws Exception
   {
      super.tearDown();
      registry.unbind("//" + host + "/RMIServer");
   }

   public static void main(String[] args)
   {
      PerformanceServerTest test = new RMIPerformanceServer();
      try
      {
         test.setUp();
         Thread.currentThread().sleep(300000);
         test.tearDown();
         //System.exit(0);
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }

}