/***************************************
 *                                     *
 *  JBoss: The OpenSource J2EE WebOS   *
 *                                     *
 *  Distributable under LGPL license.  *
 *  See terms of license at gnu.org.   *
 *                                     *
 ***************************************/
package org.jboss.test.remoting.transporter.proxy.local;

import org.jboss.jrunit.harness.TestDriver;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class TransporterProxyLocalTestCase extends TestDriver
{
   public void declareTestClasses()
   {
      addTestClasses(Client.class.getName(),
                     1,
                     Server.class.getName());
   }

}