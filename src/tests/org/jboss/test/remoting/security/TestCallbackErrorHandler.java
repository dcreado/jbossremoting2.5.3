
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
package org.jboss.test.remoting.security;

import java.util.Map;

import org.apache.log4j.Logger;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.ServerInvokerCallbackHandler;

/**
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright May 5, 2008
 * </p>
 */
public class TestCallbackErrorHandler implements TestCallbackErrorHandlerMBean
{
   private static Logger log = Logger.getLogger(TestCallbackErrorHandler.class);
   
   int counter = 0;
   
   public void handleError(Throwable ex) throws Throwable
   {
      counter++;
      log.info("handling exception: " + ex.getMessage());
      throw ex;
   }

   public void setCallbackHandler(ServerInvokerCallbackHandler serverInvokerCallbackHandler) {}
   public void setConfig(Map errorHandlerConfig) {}
   public void setServerInvoker(ServerInvoker owner) {}
   
   public int getCounter()
   {
      return counter;
   }
}

