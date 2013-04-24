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
package org.jboss.test.remoting.shutdown;

import java.util.HashMap;
import java.util.Map;

/** 
 * This class is derived from AbstractClient, and its overriding method daemon() returns
 * false.  That is, AbstractClient will create a long running daemon thread which should
 * prevent it from terminating.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1961 $
 * <p>
 * Copyright Jan 19, 2007
 * </p>
 */
public class HangingClient extends AbstractClient
{
   public HangingClient(String transport, Map config)
   {
      super(transport, config);
   }
   
   
   public static void main(String[] args)
   {
      try
      {
         if (args.length == 0)
            throw new RuntimeException();
         
         String transport = args[0];
         
         HashMap config = new HashMap();
         System.out.println("args.length: " + args.length);
         if (args.length > 1)
            getConfig(config, args[1]);
         
         AbstractClient client = new HangingClient(transport, config);
         client.testShutdown();
      }
      catch (Throwable t)
      {
         t.printStackTrace();
      }
   }
   

   protected boolean daemon()
   {
      return false;
   }

}
