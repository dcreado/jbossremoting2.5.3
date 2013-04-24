/***************************************
 *                                     *
 *  JBoss: The OpenSource J2EE WebOS   *
 *                                     *
 *  Distributable under LGPL license.  *
 *  See terms of license at gnu.org.   *
 *                                     *
 ***************************************/
package org.jboss.test.remoting.transporter.proxy.local;

import junit.framework.TestCase;
import org.jboss.remoting.transporter.TransporterClient;
import org.jboss.remoting.transporter.TransporterServer;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class Client extends TestCase
{
   private TransporterServer server;
   private DateProcessor dateProcessor;

   public void setUp() throws Exception
   {
      server = TransporterServer.createTransporterServer("socket://localhost:5400", new DateProcessorImpl(), DateProcessor.class.getName());
      dateProcessor = (DateProcessor) TransporterClient.createTransporterClient("socket://localhost:5400", DateProcessor.class);
   }

   public void testDateFormatting() throws Exception
   {
      DateConsumer consumer = (DateConsumer) TransporterClient.createTransporterClient("socket://localhost:5401", DateConsumer.class);
      String response = consumer.consumeDate(dateProcessor);
      System.out.println("Response from date consumer is " + response);
      assertTrue("The DateProcessor within this jvm should have been called to format date and was not.", DateProcessorImpl.formatDateCalled);
   }

   public void tearDown()
   {
      if (dateProcessor != null)
      {
         TransporterClient.destroyTransporterClient(dateProcessor);
      }
      if (server != null)
      {
         server.stop();
      }
   }

   public static void main(String[] args)
   {
      try
      {
         Client client = new Client();
         client.setUp();
         client.testDateFormatting();
         client.tearDown();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }
}