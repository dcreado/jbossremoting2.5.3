/***************************************
 *                                     *
 *  JBoss: The OpenSource J2EE WebOS   *
 *                                     *
 *  Distributable under LGPL license.  *
 *  See terms of license at gnu.org.   *
 *                                     *
 ***************************************/
package org.jboss.test.remoting.performance.spring.rmi;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class MultiThreadedSpringRMIPerformanceTestCase extends SpringRMIPerformanceTestCase
     {
        protected String getClientTestClass()
        {
           return MultiThreadedSpringRMIPerformanceClient.class.getName();
        }

}