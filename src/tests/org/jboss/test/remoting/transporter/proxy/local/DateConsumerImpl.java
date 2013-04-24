/***************************************
 *                                     *
 *  JBoss: The OpenSource J2EE WebOS   *
 *                                     *
 *  Distributable under LGPL license.  *
 *  See terms of license at gnu.org.   *
 *                                     *
 ***************************************/
package org.jboss.test.remoting.transporter.proxy.local;

import java.util.Date;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class DateConsumerImpl implements DateConsumer
{
   public String consumeDate(DateProcessor dateProcessor)
   {
      Date currentDate = new Date();
      String formattedDate = dateProcessor.formatDate(currentDate);
      System.out.println("called on date processor to format date.  response is " + formattedDate);
      return formattedDate;
   }
}