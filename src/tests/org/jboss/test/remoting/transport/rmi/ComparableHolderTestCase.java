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
package org.jboss.test.remoting.transport.rmi;

import java.lang.reflect.Constructor;
import java.net.InetAddress;

import junit.framework.TestCase;

import org.jboss.logging.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transport.rmi.RemotingRMIClientSocketFactory;

/** 
 * ComparableHolderTestCase verifies that the error documented in JBREM-697
 * has been fixed.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2064 $
 * <p>
 * Copyright Feb 4, 2007
 * </p>
 */
public class ComparableHolderTestCase extends TestCase
{
   private Logger log = Logger.getLogger(ComparableHolderTestCase.class);
   
   public void testComparableHolderEquals() throws Exception
   {
      log.info("entering " + getName());
      String localHostName = InetAddress.getLocalHost().getHostName();
      String localHostAddress = InetAddress.getLocalHost().getHostAddress();
      log.info("local host name: " + localHostName);
      log.info("local host address: " + localHostAddress);
      InvokerLocator locator1 = new InvokerLocator("socket://" + localHostName + ":2345");
      InvokerLocator locator2 = new InvokerLocator("socket://" + localHostAddress + ":2345");
      log.info("locator1: " + locator1);
      log.info("locator2: " + locator2);
      Class[] classes = RemotingRMIClientSocketFactory.class.getDeclaredClasses();
      assertEquals(1, classes.length);
      Class ComparableHolder = classes[0];
      Constructor cons = ComparableHolder.getConstructor(new Class[] {InvokerLocator.class});
      Object holder1 = cons.newInstance(new Object[] {locator1});
      Object holder2 = cons.newInstance(new Object[] {locator2});
      assertEquals(holder1, holder2);
      assertEquals(holder1.hashCode(), holder2.hashCode());
      log.info(getName() + " PASSES");
   }
}
