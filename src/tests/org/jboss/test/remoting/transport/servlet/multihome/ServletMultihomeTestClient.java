
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
package org.jboss.test.remoting.transport.servlet.multihome;

import java.util.Map;

import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.test.remoting.multihome.MultihomeTestParent;

/**
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Jan 13, 2008
 * </p>
 */
public class ServletMultihomeTestClient extends MultihomeTestParent
{
   protected String getTransport()
   {
      return "servlet";
   }
   
   protected void setupServer() throws Exception
   {
      locatorURI = getTransport() + "://" + InvokerLocator.MULTIHOME + getPath() + "/?";
      locatorURI += InvokerLocator.CONNECT_HOMES_KEY + "=localhost:7071!localhost:7082!localhost:7093";
      serverLocator = new InvokerLocator(locatorURI);
      log.info("server locator: " + locatorURI);
   }
   
   
   protected String getPath()
   {
      return "/servlet-invoker/ServerInvokerServlet";
   }
   
   protected void addExtraCallbackConfig(Map config)
   {
      config.put(Client.CALLBACK_SERVER_PROTOCOL, "http");
   }
}

