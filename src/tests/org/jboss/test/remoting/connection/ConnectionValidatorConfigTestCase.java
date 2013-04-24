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

package org.jboss.test.remoting.connection;

import org.jboss.jrunit.harness.TestDriver;

/**
 * ConnectionValidatorConfigTestCase verifies that parameters are correctly set
 * on org.jboss.remoting.ConnectionValidator.
 * 
 * See JBREM-755.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2473 $
 * <p>
 * Copyright Jun 16, 2007
 * </p>
 */
public class ConnectionValidatorConfigTestCase extends TestDriver
{
   public void declareTestClasses()
   {
      addTestClasses(ConnectionValidatorConfigTestClient.class.getName(),
                     1,
                     ConnectionValidatorConfigTestServer.class.getName());
   }
}