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
 * Created on Jan 3, 2006
 */
package org.jboss.test.remoting.configuration;

import org.jboss.logging.Logger;
import org.jboss.remoting.InvokerRegistry;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.transport.Connector;

import junit.framework.TestCase;


/**
 * A MiscellaneousParametersTestCase.
 
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * @version $Revision: 594 $
 * <p>
 * Copyright (c) 2005
 * </p>
 */

public class MiscellaneousParametersTestCase extends TestCase
{
   protected static final Logger log = Logger.getLogger(MiscellaneousParametersTestCase.class);
   
   public void testMiscellaneousParameters()
   {
      boolean success = true;
      
      try
      {
         Connector connector = new Connector();
         String onewayThreadPoolClassName = OnewayThreadPoolForConfigurationTest.class.getName();
         String uri = "socket://localhost/?maxNumThreadsOneway=3&onewayThreadPool=" + onewayThreadPoolClassName;
         connector.setInvokerLocator(uri);
         connector.create();
         connector.start();
         
         ServerInvoker[] serverInvokers = InvokerRegistry.getServerInvokers();
         
         if (serverInvokers == null || serverInvokers.length == 0)
         {
            log.error("no invoker created: " + uri);
            success = false;
         }
         
         ServerInvoker serverInvoker = serverInvokers[0];
         int maxNumThreadsOneWay = serverInvoker.getMaxNumberOfOnewayThreads();
         String returnedClassName = serverInvoker.getOnewayThreadPool().getClass().getName();
         
         if (maxNumThreadsOneWay != 3)
         {
            log.error("maxNumThreadsOneWay (" + maxNumThreadsOneWay + ") != expected value (" + 3);
            success = false;
         }
         
         if (returnedClassName != onewayThreadPoolClassName)
         {
            log.error("onewayThreadPool (" + returnedClassName + ") != expected value (" + onewayThreadPoolClassName);
            success = false;
         }
         
         connector.stop();
         connector.destroy();
         
         assertTrue(success);
      }
      catch (Exception e)
      {
         e.printStackTrace();
         log.error(e);
         fail();
      }
   }

}

