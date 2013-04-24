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
package org.jboss.test.remoting.shutdown;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;

/** 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 3011 $
 * <p>
 * Copyright Jan 19, 2007
 * </p>
 */
public class OpenClient extends TestCase
{    
   private static Logger log = Logger.getLogger(OpenClient.class);
   private String transport;
   private Map extraConfig;
   private Client client;
   
   
   public OpenClient(String transport, Map config)
   {
      this.transport = transport;
      this.extraConfig = new HashMap(config);
      log.info("client transport: " + transport);
      Runtime.getRuntime().traceMethodCalls(true);
   }
   
   
   /**
    * This test is used to verify that a server can shut down even if a client does not.
    */
   public void testShutdown() throws Throwable
   {
      try
      {
         String host = InetAddress.getLocalHost().getHostAddress();
         String portString = System.getProperty("port");
         int port = Integer.parseInt(portString);
         String locatorURI = transport + "://" + host + ":" + port;
         InvokerLocator locator = new InvokerLocator(locatorURI);
         HashMap clientConfig = new HashMap(extraConfig);
         client = new Client(locator, clientConfig);
         client.connect();
         log.info("client connected");
         log.info("READY");
         
         for (int i = 0; i < 5; i++)
         {
            new InvokerThread(i).start();
         }
         
         log.info("going to sleep");
         log.info("*******************************************************");
         log.info("****************   EXPECT EXCEPTIONS   ****************");
         log.info("*******************************************************");
         Thread.sleep(60000);
         log.info("calling disconnect()");
         client.disconnect();
         log.info("client disconnected");
      }
      catch (Exception e)
      {
         log.info("exception in client: " + e);
         System.exit(1);
      }
   }
   
   
   protected void addCallbackArgs(Map map)
   {
      return;
   }
   
   
   protected static void getConfig(Map config, String configs)
   {
      int start = 0;
      int ampersand = configs.indexOf('&');
      while (ampersand > 0)
      {
         String s = configs.substring(start, ampersand);
         int equals = s.indexOf('=');
         String param = s.substring(0, equals);
         String value = s.substring(equals + 1);
         config.put(param, value);
         start = ampersand + 1;
         ampersand = configs.indexOf('&', start);
      }
   }
   

   public static void main(String[] args)
   {
      try
      {
         if (args.length == 0)
            throw new RuntimeException();
         
         String transport = args[0];
         
         HashMap config = new HashMap();
         System.out.println("args.length: " + args.length);
         if (args.length > 1)
            getConfig(config, args[1]);
         
         OpenClient client = new OpenClient(transport, config);
         client.testShutdown();
      }
      catch (Throwable t)
      {
         t.printStackTrace();
      }
   }
   
   
   public class InvokerThread extends Thread
   {
      private int id;
      
      public InvokerThread(int id)
      {
         this.id = id;
         setName("InvokerThread:" + id);
      }
      
      public void run()
      {
         try
         {
            log.info("client " + id + " making invocation");
            client.invoke(new Integer(id));
            log.info("client " + id + " made invocation");
         }
         catch (Throwable e)
         {
            e.printStackTrace();
         }
      }
   }
}
