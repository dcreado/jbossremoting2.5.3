/***************************************
 *                                     *
 *  JBoss: The OpenSource J2EE WebOS   *
 *                                     *
 *  Distributable under LGPL license.  *
 *  See terms of license at gnu.org.   *
 *                                     *
 ***************************************/
package org.jboss.test.remoting.transporter.proxy.local;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public interface DateConsumer
{
   public String consumeDate(DateProcessor dateProcessor);
}