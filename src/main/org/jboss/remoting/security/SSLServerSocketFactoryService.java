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

package org.jboss.remoting.security;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;

/**
 * This is a basic wrapper around the SSLSocketBuilder which is needed
 * because it extneds the javax.net.ServerSocketFactory class and
 * implements the SSLServerSocketFactoryServiceMBean.  It has no other function.
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class SSLServerSocketFactoryService extends CustomSSLServerSocketFactory implements SSLServerSocketFactoryServiceMBean
{
   public SSLServerSocketFactoryService()
   {
      super();
   }

   /**
    * Constructor for {@link CustomSSLServerSocketFactory}. The factory can be <code>null</code> - call
    * {@link #setFactory(javax.net.ssl.SSLServerSocketFactory)} to set it later.
    *
    * @param factory the true factory this class delegates to
    * @param builder the class that built this custom factory - contains all the configuration for this factory
    */
   public SSLServerSocketFactoryService(SSLServerSocketFactory factory, SSLSocketBuilderMBean builder)
   {
      super(factory, builder);
   }

   /**
    * start the service, create is already called
    */
   public void start() throws Exception
   {
      if(getSSLSocketBuilder() != null)
      {
         ServerSocketFactory svrSocketFactory = getSSLSocketBuilder().createSSLServerSocketFactory();
         if(svrSocketFactory instanceof SSLServerSocketFactory)
         {
            setFactory(((SSLServerSocketFactory)svrSocketFactory));
         }
         else
         {
            throw new Exception("Can not start server socket factory service as server socket factory produces is not SSL based.");
         }
      }
      else
      {
         throw new Exception("Can not create server socket factory due to the SSLSocketBuilder not being set.");
      }
   }

   /**
    * create the service, do expensive operations etc
    */
   public void create() throws Exception
   {
      //NOOP
   }

   /**
    * stop the service
    */
   public void stop()
   {
      //NOOP
   }

   /**
    * destroy the service, tear down
    */
   public void destroy()
   {
      //NOOP
   }
}