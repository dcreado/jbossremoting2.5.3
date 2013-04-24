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
package org.jboss.test.remoting.detection.jndi;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Properties;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import junit.framework.TestCase;

import org.jboss.logging.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.detection.Detection;
import org.jboss.remoting.detection.jndi.JNDIDetector;
import org.jboss.remoting.util.SecurityUtility;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @author <a href="mailto:mazz@jboss.com">John Mazzitelli</a>
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 */
public class CleanDetectionTestClient extends TestCase //implements NotificationListener
{
   private static Logger log = Logger.getLogger(CleanDetectionTestClient.class);
   
   private String detectorHost;
   private int detectorPort = 1099;
   private String contextFactory = "org.jnp.interfaces.NamingContextFactory";
   private String urlPackage = "org.jboss.naming:org.jnp.interfaces";
   
   protected JNDIDetector detector;
   protected int serversDetected;
   protected boolean invocationSucceeded;
   protected Object lock = new Object();
   protected boolean notified;
   
   private Context context;

   
   public void testCleanDetect()
   {
      try
      {
         String host = InetAddress.getLocalHost().getHostName();
         
         Socket s = null;
         for (int i = 0; i < 5; i++)
         {
            try
            {
               s = new Socket(host, CleanDetectionTestServer.syncPort);
               break;
            }
            catch (Exception e)
            {
               log.info("Unable to connect to " + host + ":" + CleanDetectionTestServer.syncPort);
               log.info("Will try again");
               try
               {
                  Thread.sleep(2000);
               }
               catch (InterruptedException ignored) {}
            }
         }
         InputStream is = s.getInputStream();
         OutputStream os = s.getOutputStream();
         
         // Wait until server has been started.
         is.read();
         
         // Get detection message from JNDI server.
         createContext();
         NamingEnumeration enumeration = listBindings(context, "");
         assertTrue(enumeration.hasMore());
         Binding binding = (Binding) enumeration.next();
         assertFalse(enumeration.hasMore());
         log.info(binding);
         assertTrue(binding.getObject() instanceof Detection);
         Detection detection = (Detection) binding.getObject();
         assertEquals(1, detection.getLocators().length);
         InvokerLocator locator = detection.getLocators()[0];
         log.info("locator: " + locator);

         // Tell server to shut down.
         os.write(5);
         
         // Tell server to restart.
         os.write(7);
         Thread.sleep(4000);

         // Get new detection message from JNDI server.
         enumeration = listBindings(context, "");
         assertTrue(enumeration.hasMore());
         binding = (Binding) enumeration.next();
         log.info(binding);
         assertFalse(enumeration.hasMore());
         assertTrue(binding.getObject() instanceof Detection);
         detection = (Detection) binding.getObject();
         assertEquals(1, detection.getLocators().length);
         InvokerLocator newLocator = detection.getLocators()[0];
         log.info("new locator: " + newLocator);
         
         // Verify that JNDIDetector has already discovered that old server is dead and
         // has registered new server.
         assertFalse(locator.equals(newLocator));

         // Tell server test is over.
         os.write(9);
      }
      catch (Exception e)
      {
         log.error(e);
         e.printStackTrace();
         fail();
      }
   }
   
   
   private void createContext() throws Exception
   {
      detectorHost = InetAddress.getLocalHost().getHostName();
      
      Properties env = new Properties();
      env.put(Context.INITIAL_CONTEXT_FACTORY, contextFactory);
      env.put(Context.PROVIDER_URL, detectorHost + ":" + detectorPort);
      env.put(Context.URL_PKG_PREFIXES, urlPackage);

      InitialContext initialContext = createContext(env);
      
      String subContextName = JNDIDetector.DETECTION_SUBCONTEXT_NAME;
      try
      {
         context = initialContextLookup(initialContext, subContextName);
      }
      catch(NamingException e)
      {
         try
         {
            context = createSubcontext(initialContext, subContextName);
         }
         catch(NameAlreadyBoundException e1)
         {
            log.debug("The sub context " + subContextName + " was created before we could.");
            context = initialContextLookup(initialContext, subContextName);
         }
      }
   }
   
   static private Context createSubcontext(final InitialContext initialContext, final String subContextName)
   throws NamingException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return initialContext.createSubcontext(subContextName);
      }

      try
      {
         return (Context) AccessController.doPrivileged( new PrivilegedExceptionAction() 
         {
            public Object run() throws NamingException
            {
               return initialContext.createSubcontext(subContextName);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (NamingException) e.getCause();
      }
   }
   
   static private Context initialContextLookup(final InitialContext initialContext, final String subContextName)
   throws NamingException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return (Context) initialContext.lookup(subContextName);
      }

      try
      {
         return (Context) AccessController.doPrivileged( new PrivilegedExceptionAction() 
         {
            public Object run() throws NamingException
            {
               return initialContext.lookup(subContextName);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (NamingException) e.getCause();
      }
   }
   
   static private NamingEnumeration listBindings(final Context context, final String bindName)
   throws NamingException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return context.listBindings(bindName);
      }

      try
      {
         return (NamingEnumeration) AccessController.doPrivileged( new PrivilegedExceptionAction() 
         {
            public Object run() throws NamingException
            {
               return context.listBindings(bindName);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (NamingException) e.getCause();
      }
   }
   
   static private InitialContext createContext(final Properties env) throws NamingException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return new InitialContext(env);
      }
      
      try
      {
         return (InitialContext) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return new InitialContext(env);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RuntimeException) e.getCause();
      }
   }
}
