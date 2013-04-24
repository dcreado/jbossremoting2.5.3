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
package org.jboss.test.remoting.soak;

/**
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Feb 24, 2008
 * </p>
 */
public class SoakConstants
{
   public static final int SENDERS = 5; 
   
   public static final int CALLBACK_LISTENERS = 5;
   
   public static final int DURATION = 12 * 60 * 60 * 1000;
   
   public static final int INTERVAL = 1000000;
   
   public static final String locator = "://localhost:5678";
   
   public static final String NUMBER_OF_CALLS = "numberOfCalls";
   
   public static final String NAME = "name";
   
   public static final String COPY = "copy";
   
   public static final String SPIN = "spin";
   
   public static final String CALLBACK = "callback";
   
   public static final String NUMBER_OF_CALLBACKS = "numberOfCallbacks";
   
   public static final String PAYLOAD = "payload";
   
   public static final String SPIN_TIME = "spinTime";
   
   public static final String COUNTER = "counter";
   
   public static final String FAILURE_COUNTER = "failureCounter";
   
   public static final String IN_USE_SET = "inUseSet";
}

