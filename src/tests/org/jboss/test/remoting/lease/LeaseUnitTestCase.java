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
package org.jboss.test.remoting.lease;

import junit.framework.TestCase;
import org.apache.log4j.Level;
import org.jboss.logging.XLevel;
import org.jboss.remoting.ConnectionNotifier;
import org.jboss.remoting.Lease;

import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class LeaseUnitTestCase extends TestCase
{

   public void setUp()
   {
      org.apache.log4j.BasicConfigurator.configure();
      org.apache.log4j.Category.getRoot().setLevel(Level.INFO);
      org.apache.log4j.Category.getInstance("org.jboss.remoting").setLevel(XLevel.TRACE);
      org.apache.log4j.Category.getInstance("org.jgroups").setLevel(Level.FATAL);
   }

   public void testLease() throws Exception
   {
      ConnectionNotifierMock notifier = new ConnectionNotifierMock();
      Lease lease = new Lease("123", 5000, "foobar", null, notifier, null);
      lease.startLease();

      System.out.println("test - lease started");

      System.out.println("test - sleeping 3 seconds");
      Thread.currentThread().sleep(3000);

      assertFalse(notifier.notificationFired);

      System.out.println("test - update lease");
      lease.updateLease(5000);

      System.out.println("test - sleeping 5 seconds");
      Thread.currentThread().sleep(5000);

      assertFalse(notifier.notificationFired);

      System.out.println("test - update lease");
      lease.updateLease(5000);

      System.out.println("test - sleeping 10 seconds");
      Thread.currentThread().sleep(10000);

      assertFalse(notifier.notificationFired);

      System.out.println("test - update lease");
      lease.updateLease(5000);

      System.out.println("test - sleeping 60 seconds");
      Thread.currentThread().sleep(60000);

      assertTrue(notifier.notificationFired);

   }

   public void testMultipleLeases() throws Exception
   {
      ConnectionNotifierMock notifier1 = new ConnectionNotifierMock();
      ConnectionNotifierMock notifier2 = new ConnectionNotifierMock();
      ConnectionNotifierMock notifier3 = new ConnectionNotifierMock();
      Lease lease1 = new Lease("123", 3000, "foo", null, notifier1, null);
      Lease lease2 = new Lease("456", 5000, "bar", null, notifier2, null);
      Lease lease3 = new Lease("789", 10000, "foobar", null, notifier3, null);
      lease1.startLease();
      lease2.startLease();
      lease3.startLease();

      // when first started, the lease window is lease period X 2
      System.out.println("waiting 5 seconds before update.");
      Thread.currentThread().sleep(5000);

      assertFalse(notifier1.notificationFired);
      assertFalse(notifier2.notificationFired);
      assertFalse(notifier3.notificationFired);

      lease1.updateLease(3000);
      lease2.updateLease(5000);
      lease3.updateLease(10000);

      // having to so 5 second sleep again, because lease window
      // not activated until after the first update is made
      System.out.println("waiting 5 seconds before update.");
      Thread.currentThread().sleep(5000);

      assertFalse(notifier1.notificationFired);
      assertFalse(notifier2.notificationFired);
      assertFalse(notifier3.notificationFired);

      lease1.updateLease(3000);
      lease2.updateLease(5000);
      lease3.updateLease(10000);


      // now initial delay is gone (since update with the same delay times),
      // however, the lease window is bigger (should be 10 seconds) since took so long to update,
      // therefore, should be able to wait 8 seconds now without firing
      System.out.println("waiting 8 seconds before update.");
      Thread.currentThread().sleep(8000);

      assertFalse(notifier1.notificationFired);
      assertFalse(notifier2.notificationFired);
      assertFalse(notifier3.notificationFired);

      // will let lease1 timeout, but update lease 2 & 3 to new times
      // which should reset the delay window to new value of 6 seconds
      lease2.updateLease(3000);
      lease3.updateLease(3000);

      // will be waiting 2 seconds and then updating lease 2 & 3
      // several times as we don't want to change the lease window
      // for them, but have to allow lease 1 to timeout (and since it's
      // lease window is now 10 and requires two internal timeouts on the
      // lease before the notification is fired, means need to wait a total
      // of 20 seonds)
      System.out.println("waiting 2 seconds before update");
      Thread.currentThread().sleep(2000);

      // note, should *not* change the lease window
      lease2.updateLease(3000);
      lease3.updateLease(3000);

      System.out.println("waiting 2 seconds before update");
      Thread.currentThread().sleep(2000);

      // note, should *not* change the lease window
      lease2.updateLease(3000);
      lease3.updateLease(3000);

      System.out.println("waiting 2 seconds before update");
      Thread.currentThread().sleep(2000);

      // note, should *not* change the lease window
      lease2.updateLease(3000);
      lease3.updateLease(3000);

      System.out.println("waiting 2 seconds before update");
      Thread.currentThread().sleep(2000);

      // note, should *not* change the lease window
      lease2.updateLease(3000);
      lease3.updateLease(3000);

      System.out.println("waiting 2 seconds before update");
      Thread.currentThread().sleep(2000);

      // note, should *not* change the lease window
      lease2.updateLease(3000);
      lease3.updateLease(3000);

      System.out.println("waiting 2 seconds before update");
      Thread.currentThread().sleep(2000);

      // note, should *not* change the lease window
      lease2.updateLease(3000);
      lease3.updateLease(3000);

      System.out.println("waiting 2 seconds before update");
      Thread.currentThread().sleep(2000);

      // should be total of 22 seconds for lease 1, so should have timeout
      assertTrue(notifier1.notificationFired);
      assertFalse(notifier2.notificationFired);
      assertFalse(notifier3.notificationFired);

      // will let lease2 timout now, but update lease 3, but use same time.
      lease3.updateLease(3000);

      System.out.println("waiting 4 seonds before update");
      Thread.currentThread().sleep(4000);

      lease3.updateLease(3000);

      System.out.println("4 seconds for lease 2 timeout");
      Thread.currentThread().sleep(4000);

      lease3.updateLease(3000);

      System.out.println("4 seconds for lease 2 timeout");
      Thread.currentThread().sleep(4000);

      assertTrue(notifier2.notificationFired);
      assertFalse(notifier3.notificationFired);

      // now will let lease 3 timeout

      System.out.println("waiting 15 seconds for lease 3 timeout");
      Thread.currentThread().sleep(15000);

      assertTrue(notifier3.notificationFired);

   }

   public static class ConnectionNotifierMock extends ConnectionNotifier
   {
      public boolean notificationFired = false;

      public void connectionLost(String locatorurl, String clientSessionId, Map requestPayload)
      {
         System.out.println("connection lost");
         notificationFired = true;
      }

      public void connectionTerminated(String locatorURL, String clientSessionId, Map requestPayload)
      {
         System.out.println("connection terminate");
         notificationFired = true;
      }
   }
}