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
package org.jboss.test.remoting.transport.bisocket.configuration;

import java.util.Map;

import org.jboss.remoting.transport.bisocket.Bisocket;
import org.jboss.test.remoting.transport.config.FactoryConfigTestCaseParent;

/**
 * 
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 * <p>
 * Copyright (c) Dec 15, 2005
 * </p>
 */
public class FactoryConfigTestCase extends FactoryConfigTestCaseParent
{
   protected String getTransport()
   {
      return "bisocket";
   }
   
   protected void configureServer(Map config)
   {
      config.put(Bisocket.IS_CALLBACK_SERVER, "false");
   }
}
