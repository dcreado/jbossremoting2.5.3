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

package org.jboss.remoting;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.jboss.remoting.util.SecurityUtility;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class Version
{
   // possible remoting versions
   public static final byte VERSION_1 = 1;
   public static final byte VERSION_2 = 2;
   public static final byte VERSION_2_2 = 22;

   public static final String VERSION = "2.5.3 (Flounder)";
   private static final byte byteVersion = VERSION_2_2;
   private static byte defaultByteVersion = byteVersion;
   private static boolean performVersioning = true;


   public static final String PRE_2_0_COMPATIBLE = "jboss.remoting.pre_2_0_compatible";
   //TODO: -TME Is this the best system property key to use?  May want to use something that
   // is more decscriptive that is user defined version.  However, may want to make available
   // to users via system property the version of remoting?
   public static final String REMOTING_VERSION_TO_USE = "jboss.remoting.version";

   // have a static block to load the user defined version to use
   static
   {
      try
      {
         ClassLoader cl = getClassLoader();
         Class c = cl.loadClass("org.jboss.logging.Logger");
         Method getLogger = c.getMethod("getLogger", new Class[]{String.class});
         Object logger = getLogger.invoke(null, new Object[] {"org.jboss.remoting"});
         Method debug = c.getMethod("debug", new Class[]{Object.class});
         debug.invoke(logger, new Object[]{"Remoting version: " + VERSION});
      }
      catch (Throwable t)
      {
         // ignore
      }
      
      boolean precompatibleFlag = false;
      String  precompatible = getSystemProperty(PRE_2_0_COMPATIBLE);

      if(precompatible != null && precompatible.length() > 0)
      {
         precompatibleFlag = Boolean.valueOf(precompatible).booleanValue();
      }
      // if is precompatible, no need to look for custom version, as there is only 1 precompatible version
      if(precompatibleFlag)
      {
         defaultByteVersion = 1;
         performVersioning = false;
      }
      else
      {
         String userDefinedVersion = getSystemProperty(REMOTING_VERSION_TO_USE);
         
         if(userDefinedVersion != null && userDefinedVersion.length() > 0)
         {
            byte userByteVersion = new Byte(userDefinedVersion).byteValue();
            if(userByteVersion > 0)
            {
               defaultByteVersion = userByteVersion;
               if(defaultByteVersion < 2)
               {
                  performVersioning = false;
               }
            }
            else
            {
               System.err.println("Can not set remoting version to value less than 1.  " +
                                  "System property value set for '" + REMOTING_VERSION_TO_USE + "' was " + userDefinedVersion);
            }
         }
         else
         {
            setSystemProperty(REMOTING_VERSION_TO_USE, new Byte(defaultByteVersion).toString());
         }
      }
   }

   public static void main(String arg[])
   {
      System.out.println("JBossRemoting Version " + VERSION);
   }

   public static int getDefaultVersion()
   {
      return defaultByteVersion;
   }

   public static boolean performVersioning()
   {
      return performVersioning;
   }
   
   public static boolean performVersioning(int version)
   {
      return version >= 2;
   }
   
   public static boolean isValidVersion(int version)
   {
      return version == VERSION_1 || version == VERSION_2 || version == VERSION_2_2;
   }
   
   static private String getSystemProperty(final String name)
   {
      if (SecurityUtility.skipAccessControl())
         return System.getProperty(name);
      
      String value = null;
      try
      {
         value = (String)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return System.getProperty(name);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RuntimeException) e.getCause();
      }
      
      return value;
   }
   
   static private void setSystemProperty(final String name, final String value)
   {
      if (SecurityUtility.skipAccessControl())
      {
         System.setProperty(name, value);
         return;
      }
      
      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return System.setProperty(name, value);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RuntimeException) e.getCause();
      }
   }
   
   static private ClassLoader getClassLoader()
   {
      if (SecurityUtility.skipAccessControl())
      {
         return Version.class.getClassLoader();
      }

      return (ClassLoader)AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            return Version.class.getClassLoader();
         }
      });
   }
}