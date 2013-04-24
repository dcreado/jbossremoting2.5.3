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
package org.jboss.test.remoting.soak;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.logging.XLevel;


/**
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Mar 13, 2008
 * </p>
 */
public class ClientLauncher extends SoakConstants
{
   private static Logger log = Logger.getLogger(ClientLauncher.class);
   
   private static Set mockEJBClientsInUse = Collections.synchronizedSet(new HashSet());
   private static Set mockJBMClientsInUse = Collections.synchronizedSet(new HashSet());
   private static Set heavyComputeClientsInUse = Collections.synchronizedSet(new HashSet());
   
   private static Counter mockEJBClientCounter = new Counter();
   private static Counter mockJBMClientCounter = new Counter();
   private static Counter heavyComputeClientCounter = new Counter();
   
   private static Counter mockEJBClientFailureCounter = new Counter();
   private static Counter mockJBMClientFailureCounter = new Counter();
   private static Counter heavyComputeClientFailureCounter = new Counter();
   
   private static Random random;
   private static String[] transports = {"bisocket", "http", "rmi", "socket"};
   private static int[] ports = {6666, 6667, 6668, 6669};
   private static int[] transportCounters = new int[4];
   private static String host;
   private static boolean creationDone;
   
   // Configuration parameters.
   private static int MAX_CLIENTS = 30;
   private static int NUMBER_OF_EJB_CALLS = 4000;
   private static int NUMBER_OF_JBM_CALLS = 2000;
   private static int NUMBER_OF_JBM_CALLBACKS = 4;
   private static int NUMBER_OF_HEAVY_COMPUTE_CALLS = 5;
   private static String HEAVY_COMPUTE_SPIN_TIME = "4000";
   
   
   public static void main(String[] args)
   {
      try
      {
         Logger.getLogger("org.jboss.remoting").setLevel(XLevel.INFO);
         Logger.getLogger("org.jboss.test.remoting").setLevel(Level.INFO);
         String pattern = "[%d{ABSOLUTE}] [%t] %5p (%F:%L) - %m%n";
         PatternLayout layout = new PatternLayout(pattern);
         ConsoleAppender consoleAppender = new ConsoleAppender(layout);
         Logger.getRootLogger().addAppender(consoleAppender); 
         
         host = InetAddress.getLocalHost().getHostAddress();
         random = new Random(System.currentTimeMillis());
         Timer timer = new Timer(false);
         timer.schedule(new DisplayTimerTask(), 30000, 30000);
         long start = System.currentTimeMillis();

         while (System.currentTimeMillis() - start < DURATION)
         {
            if (mockEJBClientsInUse.size() + 
                  mockJBMClientsInUse.size() +
                  heavyComputeClientsInUse.size() < MAX_CLIENTS)
            {
               createClient();
            }
            
            int n = random.nextInt(40) * 100;
            log.debug("waiting " + n + " ms");
            Thread.sleep(n);
         }
         
         creationDone = true;
         log.info("Client creation phase complete.");
      }
      catch (Throwable t)
      {
         log.error("Error", t);
      }
   }
   
   
   protected static void createClient() throws Throwable
   {
      int k = random.nextInt(4);
      String transport = transports[k];
      int port = ports[k];
      transportCounters[k]++;
      int clientType = random.nextInt(25);
      
      if (clientType < 10)
      {
         createMockEJBClient(transport, port);
      }
      else if (clientType < 20)
      {
         createMockJBMClient(transport, port);
      }
      else
      {
         createHeavyComputeClient(transport, port);
      }
   }
   
   
   protected static void createMockEJBClient(String transport, int port) throws Throwable
   {
      String locatorURI = transport + "://" + host + ":" + port + "/?timeout=0";
      Map metadata = new HashMap();
      metadata.put(NAME, "MockEJBClient" + mockEJBClientCounter.increment() + "[" + transport + "]");
      metadata.put(IN_USE_SET, mockEJBClientsInUse);
      metadata.put(FAILURE_COUNTER, mockEJBClientFailureCounter);
      metadata.put(NUMBER_OF_CALLS, Integer.toString(random.nextInt(NUMBER_OF_EJB_CALLS)));
      MockEJBClient c = new MockEJBClient(locatorURI, metadata);
      Thread t = new Thread(c);
      t.start(); 
   }
   
   
   protected static void createMockJBMClient(String transport, int port) throws Throwable
   {
      String locatorURI = transport + "://" + host + ":" + port + "/?timeout=0";
      Map metadata = new HashMap();
      metadata.put(NAME, "MockJBMClient" + mockJBMClientCounter.increment() + "[" + transport + "]");
      metadata.put(IN_USE_SET, mockJBMClientsInUse);
      metadata.put(FAILURE_COUNTER, mockJBMClientFailureCounter);
      metadata.put(NUMBER_OF_CALLS, Integer.toString(random.nextInt(NUMBER_OF_JBM_CALLS)));
      metadata.put(NUMBER_OF_CALLBACKS, Integer.toString(random.nextInt(NUMBER_OF_JBM_CALLBACKS)));
      MockJBMClient c = new MockJBMClient(locatorURI, metadata);
      Thread t = new Thread(c);
      t.start();
   }
   
   
   protected static void createHeavyComputeClient(String transport, int port) throws Throwable
   {
      String locatorURI = transport + "://" + host + ":" + port + "/?timeout=0";
      Map metadata = new HashMap();
      metadata.put(NAME, "HeavyComputeClient" + heavyComputeClientCounter.increment() + "[" + transport + "]");
      metadata.put(IN_USE_SET, heavyComputeClientsInUse);
      metadata.put(FAILURE_COUNTER, heavyComputeClientFailureCounter);
      metadata.put(NUMBER_OF_CALLS, Integer.toString(random.nextInt(NUMBER_OF_HEAVY_COMPUTE_CALLS)));
      metadata.put(SPIN_TIME, HEAVY_COMPUTE_SPIN_TIME);
      HeavyComputeClient c = new HeavyComputeClient(locatorURI, metadata);
      heavyComputeClientCounter.increment();
      Thread t = new Thread(c);
      t.start();
   }
   
   
   static class Counter
   {
      int count;
      
      public synchronized int getCount() { return count; }
      public synchronized int increment() { return count++; }
      public synchronized int decrement() { return --count; }
   }
   
   static class DisplayTimerTask extends TimerTask
   {
      public void run()
      {
         System.out.println("");
         System.out.println("=========================================");
         System.out.println("current MockEJBCLients:      " + mockEJBClientsInUse.size());
         System.out.println("current MockJBMClients:      " + mockJBMClientsInUse.size());
         System.out.println("current HeavyComputeClients: " + heavyComputeClientsInUse.size());
         System.out.println("-----------------------------------------");
         System.out.println("bisocket clients:            " + transportCounters[0]);
         System.out.println("http clients:                " + transportCounters[1]);
         System.out.println("rmi clients:                 " + transportCounters[2]);
         System.out.println("socket clients:              " + transportCounters[3]);
         System.out.println("-----------------------------------------");
         System.out.println("failed MockEJBCLients:       " + mockEJBClientFailureCounter.getCount());
         System.out.println("failed MockJBMClients:       " + mockJBMClientFailureCounter.getCount());
         System.out.println("failed HeavyComputeClients:  " + heavyComputeClientFailureCounter.getCount());
         System.out.println("-----------------------------------------");
         printSet(mockEJBClientsInUse);
         printSet(mockJBMClientsInUse);
         printSet(heavyComputeClientsInUse);
         System.out.println("=========================================");
         System.out.println("");
         
         if (creationDone &&
               mockEJBClientsInUse.size() == 0 && 
               mockJBMClientsInUse.size() == 0 &&
               heavyComputeClientsInUse.size() == 0)

            cancel();
      }
      
      private void printSet(Set set)
      {
         synchronized (set)
         {
            Iterator it = set.iterator();
            while(it.hasNext())
            {
               System.out.println(it.next().toString());
            }
         }
      }
   }
}

