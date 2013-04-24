/***************************************
 *                                     *
 *  JBoss: The OpenSource J2EE WebOS   *
 *                                     *
 *  Distributable under LGPL license.  *
 *  See terms of license at gnu.org.   *
 *                                     *
 ***************************************/
package org.jboss.test.remoting.performance.spring.http.web;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public interface SpringHttpServer
{
   public Object makeCall(Object obj, Object param);

   public Object sendNumberOfCalls(Object obj, Object param);

}
