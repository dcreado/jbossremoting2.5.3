/***************************************
 *                                     *
 *  JBoss: The OpenSource J2EE WebOS   *
 *                                     *
 *  Distributable under LGPL license.  *
 *  See terms of license at gnu.org.   *
 *                                     *
 ***************************************/
package org.jboss.test.remoting.performance.spring.rmi;

import org.jboss.test.remoting.performance.synchronous.PerformanceServerTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import java.util.Map;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class SpringRMIPerformanceServer extends PerformanceServerTest
{
   public void init(Map metadata)
   {
      String springServiceXml = this.getClass().getResource("SpringRMIServerService.xml").getFile();

      ApplicationContext context = new FileSystemXmlApplicationContext(springServiceXml);
      SpringRMIServer server = (SpringRMIServer) context.getBean("springRMIServerService");
   }

   public static void main(String[] args)
   {
      PerformanceServerTest server = new SpringRMIPerformanceServer();
      try
      {
         server.setUp();
         Thread.currentThread().sleep(3600000);
         server.tearDown();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

}