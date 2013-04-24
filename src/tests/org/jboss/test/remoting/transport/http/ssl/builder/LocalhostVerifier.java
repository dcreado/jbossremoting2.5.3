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

package org.jboss.test.remoting.transport.http.ssl.builder;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.jboss.logging.Logger;

/**
 * 
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1162 $
 * <p>
 * Copyright (c) Jun 9, 2006
 * </p>
 */
public class LocalhostVerifier implements HostnameVerifier
{
   protected static Logger log = Logger.getLogger(LocalhostVerifier.class);
   
   public boolean verify(String s, SSLSession sslSession)
   {
      System.out.println("s: " + s);
      System.out.println("sslSession.getPeerHost(): " + sslSession.getPeerHost());
      
      try
      {
         InetAddress address1 = InetAddress.getByName(s);
         InetAddress address2 = InetAddress.getByName(sslSession.getPeerHost());
         if (address1.equals(address2))
            return true;
         else
            return false;
      }
      catch (UnknownHostException e)
      {
         return false;
      }
   }     
}
