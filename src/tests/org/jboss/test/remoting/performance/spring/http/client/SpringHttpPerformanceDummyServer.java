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
 * Created on Aug 30, 2006
 */
package org.jboss.test.remoting.performance.spring.http.client;

import org.jboss.test.remoting.performance.synchronous.PerformanceServerTest;


/**
 * The real server for the spring http performance test runs in tomcat.  This class is just
 * a dummy class passed to jrunit TestDriver.
 * 
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1424 $
 * <p>
 * Copyright (c) Aug 30, 2006
 * </p>
 */

public class SpringHttpPerformanceDummyServer extends PerformanceServerTest
{
   public static void main(String[] args)
   {
      try
      {
         Thread.sleep(3600000);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }
}

