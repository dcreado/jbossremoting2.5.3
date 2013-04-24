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
package org.jboss.remoting;

import java.util.HashMap;
import java.util.Map;


/** 
 * ServerConfiguration can hold all parameters used to configure an
 * org.jboss.remoting.transport.Connector.
 * 
 * Though it can be used programmatically, it is primarily meant to be used
 * as a replacement for the <config> xml element currently used for external
 * configuration of a Connector.  In particular, it is meant to be constructed
 * by the microcontainer from a jboss-beans.xml file and injected into the
 * Connector.
 * 
 * For an example of the use of ServerConfiguration with the microcontainer,
 * see the configuration file remoting-jboss-beans.xml in the server/default/deploy
 * directory of the JBoss Application Server 5.0.0.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 5046 $
 * <p>
 * Copyright Oct 13, 2007
 * </p>
 */
public class ServerConfiguration
{
   /**
    * transport to be used by server.
    */
   private String  transport;
   
   /**
    * Parameters that will go into InvokerLocator.   
    * <p>
    * Values MUST be of String type.
    */
   private Map invokerLocatorParameters = new HashMap();
   
   /**
    * Parameters that will be used locally by the server and will not go
    * into InvokerLocator.
    * <p>
    * Values may be of any type.
    */
   private Map serverParameters = new HashMap();
   
   /**
    * ServerInvocationHandlers.  The key is used as the subsystem name.
    * It may also be a comma separated list of subsystem names.
    */
   private Map invocationHandlers = new HashMap();
  
   
   public ServerConfiguration(String transport)
   {
      this.transport = transport;
   }
   
   public ServerConfiguration()
   {      
   }
   
   public Map getInvocationHandlers()
   {
      return invocationHandlers;
   }

   public void setInvocationHandlers(Map invocationHandlers)
   {
      this.invocationHandlers = invocationHandlers;
   }
   
   public Map getInvokerLocatorParameters()
   {
      return invokerLocatorParameters;
   }
   
   public void setInvokerLocatorParameters(Map invokerLocatorParameters)
   {
      this.invokerLocatorParameters.putAll(invokerLocatorParameters);
   }
   
   public Map getServerParameters()
   {
      return serverParameters;
   }
   
   public void setServerParameters(Map serverParameters)
   {
      this.serverParameters.putAll(serverParameters);
   }
   
   public String getTransport()
   {
      return transport;
   }

   public void setTransport(String transport)
   {
      this.transport = transport;
   }
}
