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
package org.jboss.test.remoting.detection.jndi.startup;

import junit.framework.TestCase;
import org.jboss.remoting.detection.jndi.JNDIDetector;
import org.jboss.remoting.network.NetworkInstance;
import org.jboss.remoting.network.NetworkRegistry;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import java.net.InetAddress;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class JNDIDetectorClient extends TestCase
{
   private int detectorPort = 1099;
   private String contextFactory = "org.jnp.interfaces.NamingContextFactory";
   private String urlPackage = "org.jboss.naming:org.jnp.interfaces";

   private JNDIDetector detector;
   private NetworkRegistry registry;

   public void setUp() throws Exception
   {
      String detectorHost = InetAddress.getLocalHost().getHostName();

      detector = new JNDIDetector();
      detector.setCleanDetectionNumber(2);

      System.setProperty("jboss.identity", String.valueOf(System.currentTimeMillis()));
      System.out.println("jboss.identity = " + System.getProperty("jboss.identity"));

      MBeanServer server = MBeanServerFactory.createMBeanServer();

      registry = NetworkRegistry.getInstance();
      server.registerMBean(registry, new ObjectName("remoting:type=NetworkRegistry"));

      //Need to set new domain for identity
      server.registerMBean(detector, new ObjectName("remoting:type=JNDIDetector"));

      // set config info for detector and start it.
      detector.setPort(detectorPort);
      detector.setHost(detectorHost);
      detector.setContextFactory(contextFactory);
      detector.setURLPackage(urlPackage);

   }

   public void testDetection() throws Exception
   {
      detector.start();
      Thread.sleep(4000);
      long start = System.currentTimeMillis();
      NetworkInstance[] instances = detector.forceDetection();
      long end = System.currentTimeMillis();

      System.out.println("instance = " + instances);
      System.out.println("instances.length: " + instances.length);
      System.out.println("force detection took " + (end - start) + " milliseconds.");

      assertEquals(1, instances.length);

   }

   public void tearDown() throws Exception
   {
      if (detector != null)
      {
         detector.stop();
      }
   }

}