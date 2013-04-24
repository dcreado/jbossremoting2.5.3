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

package org.jboss.remoting.serialization;

import org.jboss.logging.Logger;
import org.jboss.remoting.serialization.impl.java.JavaEncryptionSerializationManager;
import org.jboss.remoting.serialization.impl.java.JavaSerializationManager;
import org.jboss.remoting.serialization.impl.jboss.JBossEncryptionSerializationManager;
import org.jboss.remoting.util.SecurityUtility;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;

/**
 * This factory is for defining the Object stream implemenations to be used
 * along with creating those implemenations for use.  The main function will
 * be to return instance of ObjectOutput and ObjectInput.  By default, the implementations
 * will be java.io.ObjectOutputStream and java.io.ObjectInputStream.
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 * @author <a href="mailto:clebert.suconic@jboss.com">Clebert Suconic</a>
 */
public class SerializationStreamFactory implements SerializationStreamFactoryMBean
{
   protected static final Logger log = Logger.getLogger(SerializationStreamFactory.class);
   private static Map managers = new HashMap();

   public static final String DEFAULT = "default";
   public static final String JAVA = "java";
   public static final String JBOSS = "jboss";
   public static final String JAVA_ENCRYPT = "javaencrypt";
   public static final String JBOSS_ENCRYPT = "jbossencrypt";


   static
   {
      try
      {
         String defaultValue = JavaSerializationManager.class.getName();
         String managerClassName = getSystemProperty("SERIALIZATION", defaultValue);
         setManagerClassName(DEFAULT, managerClassName);
      }
      catch(Exception e)
      {
         log.error(e.getMessage(), e);
      }
      try
      {
         setManagerClassName(JAVA, JavaSerializationManager.class.getName());
      }
      catch(Exception e)
      {
         log.error(e.getMessage(), e);
      }
      try
      {
         setManagerClassName(JBOSS, "org.jboss.remoting.serialization.impl.jboss.JBossSerializationManager");
      }
      catch(Throwable e) // catching throwable as if jboss serialization not on classpath, will throw NoClassDefFoundError
      {
         log.debug("Could not load JBoss Serialization.  Use Java Serialization default.");
         log.trace(e);
      }
      try
      {
         setManagerClassName(JAVA_ENCRYPT, JavaEncryptionSerializationManager.class.getName());
      }
      catch(Throwable e) // catching throwable as if jboss serialization not on classpath, will throw NoClassDefFoundError
      {
         log.debug("Could not load Java Encrypted Serialization.  Use Java Serialization default.");
         log.trace(e);
      }
      try
      {
         setManagerClassName(JBOSS_ENCRYPT, JBossEncryptionSerializationManager.class.getName());
      }
      catch(Throwable e) // catching throwable as if jboss serialization not on classpath, will throw NoClassDefFoundError
      {
         log.debug("Could not load JBoss Encrypted Serialization.  Use Java Serialization default.");
         log.trace(e);
      }
   }

   /**
    * The fully qualified classname of the DefaultSerializationManager implementation.
    *
    * @param className
    */
   public static void setManagerClassName(String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException
   {
      setManagerClassName(DEFAULT, className);
   }

   /**
    * The fully qualified classname of the DefaultSerializationManager implementation.
    *
    * @param className
    */
   public static void setManagerClassName(String key, String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException
   {
      loadObjectManagerClass(key, className);
   }


   public String getManager()
   {
      return getManager(DEFAULT);
   }

   public String getManager(String key)
   {
      SerializationManager manager = (SerializationManager) managers.get(key);
      if(manager == null)
      {
         return null;
      }
      else
      {
         return manager.getClass().getName();
      }
   }

   public void setManager(String manager) throws Exception
   {
      setManager(DEFAULT, manager);
   }

   public void setManager(String key, String manager) throws Exception
   {
      setManagerClassName(key, manager);
   }

   /**
    * Loads the implementation class for ObjectOutput as specified by the object output class name.  Will also load
    * the constructor and cache it to be used when creating the actual implementation instance.
    */
   private static void loadObjectManagerClass(String key, String managerClassName) throws ClassNotFoundException, IllegalAccessException, InstantiationException
   {
      Class managerClass = ClassLoaderUtility.loadClass(SerializationStreamFactory.class, managerClassName);
      SerializationManager manager = (SerializationManager) managerClass.newInstance();

      if(managers.get(key) != null)
      {
         managers.remove(key);
      }
      managers.put(key, manager);
   }

   /**
    * @return the SerializationManager instance corresponding to the given key. If key is null,
    *         "java" is assumed. The method never returns null, if there's no SerializationManager
    *         associated with the given key, the method throws exception.
    *
    * @throws IOException if there's no corresponding SerializationManager instance.
    */
   public static SerializationManager getManagerInstance(String key) throws IOException
   {
      if(key == null)
      {
         key = JAVA;
      }
      SerializationManager manager = (SerializationManager) managers.get(key);

      if (manager == null)
      {
         throw new IOException("Unknown serialization type: " + key);
      }
      return manager;
   }

   public static SerializationManager getManagerInstance() throws IOException
   {
      return getManagerInstance(DEFAULT);
   }

   static private String getSystemProperty(final String name, final String defaultValue)
   {
      if (SecurityUtility.skipAccessControl())
         return System.getProperty(name, defaultValue);
         
      String value = null;
      try
      {
         value = (String)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return System.getProperty(name, defaultValue);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RuntimeException) e.getCause();
      }
      
      return value;
   }
}