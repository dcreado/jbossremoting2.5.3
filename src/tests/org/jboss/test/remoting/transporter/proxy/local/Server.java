/***************************************
 *                                     *
 *  JBoss: The OpenSource J2EE WebOS   *
 *                                     *
 *  Distributable under LGPL license.  *
 *  See terms of license at gnu.org.   *
 *                                     *
 ***************************************/
package org.jboss.test.remoting.transporter.proxy.local;

import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.remoting.transporter.TransporterServer;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class Server extends ServerTestCase
{
   private TransporterServer server;

   public void setUp() throws Exception
   {
      server = TransporterServer.createTransporterServer("socket://localhost:5401", new DateConsumerImpl(), DateConsumer.class.getName());
   }

   public void tearDown()
   {
      if (server != null)
      {
         server.stop();
      }
   }

   public static void main(String[] args)
   {
      Server svr = new Server();
      try
      {
         svr.setUp();
         Thread.currentThread().sleep(6000000);
         svr.tearDown();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }
}