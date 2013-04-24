/***************************************
 *                                     *
 *  JBoss: The OpenSource J2EE WebOS   *
 *                                     *
 *  Distributable under LGPL license.  *
 *  See terms of license at gnu.org.   *
 *                                     *
 ***************************************/
package org.jboss.test.remoting.transporter.proxy.local;

import java.text.DateFormat;
import java.util.Date;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class DateProcessorImpl
{
   public static boolean formatDateCalled = false;

   public String formatDate(Date dateToConvert)
   {
      DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
      System.out.println("Sending back to caller formated date of " + dateFormat);
      formatDateCalled = true;
      return dateFormat.format(dateToConvert);
   }

}