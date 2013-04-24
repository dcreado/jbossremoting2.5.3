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
package org.jboss.remoting.loading;

import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.util.SecurityUtility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * ClassByteClassLoader is a classloader that will allow dynamic adding of classes from a remote machine
 * to be added and visible locally.
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 * @version $Revision: 5683 $
 */
public class ClassByteClassLoader extends ClassLoader
{
   private static final Logger log = Logger.getLogger(ClassByteClassLoader.class);
   private final Map loadedClasses = Collections.synchronizedMap(new java.util.HashMap());
   private final Map loadedResources = Collections.synchronizedMap(new java.util.HashMap());
   private final ReferenceQueue queue = new ReferenceQueue();

   private Client loaderClient = null;

   public ClassByteClassLoader()
   {
      super();
   }

   public ClassByteClassLoader(ClassLoader parent)
   {
      super(parent);
   }

   public void setClientInvoker(Client loaderClient)
   {
      this.loaderClient = loaderClient;
   }

   /**
    * Will disconnect loader client if is present.
    */
   public void destroy()
   {
      if(loaderClient != null && loaderClient.isConnected())
      {
         loaderClient.disconnect();
      }
   }

   /**
    * inner class will keep an local reference to the key
    * so we can lookup the resource File that we are using
    * on deletion to ensure we've cleaned it up
    */
   private final class MyRef extends WeakReference
   {
      private final String key;

      MyRef(String key, Object obj)
      {
         super(obj);
         this.key = key;
      }
   }

   /**
    * clean an individual reference and delete any remaining
    * resources that are still being referenced by it
    */
   private void clean(MyRef myref)
   {
      loadedClasses.remove(myref.key);
      File f = (File) loadedResources.remove(myref.key);
      if(f != null)
      {
         f.delete();
      }
      myref.clear();
      myref = null;
   }

   /**
    * will walk through all objects (if any) in the ReferenceQueue and
    * make sure we've cleaned up our released class references
    */
   private void performMaintenance()
   {
      int count = 0;
      Reference ref = null;
      while((ref = queue.poll()) != null)
      {
         count++;
         MyRef myref = (MyRef) ref;
         clean(myref);
      }

      if(count > 0 && log.isTraceEnabled())
      {
         log.trace("ClassByteClassLoader reclaimed " + count + " objects");
      }
   }


   public String toString()
   {
      return "ClassByteClassLoader [" + loadedClasses + "]";
   }

   protected void finalize() throws Throwable
   {
      // make sure we're OK on the reference queue
      performMaintenance();

      java.util.Iterator iter = loadedResources.values().iterator();
      while(iter.hasNext())
      {
         ((java.io.File) iter.next()).delete();
      }
      loadedResources.clear();
      loadedClasses.clear();
      super.finalize();
   }

   public Class loadClass(final String className, ClassBytes bytes[])
         throws ClassNotFoundException, java.io.IOException
   {
      // make sure we're OK on the reference queue
      performMaintenance();

      if(log.isTraceEnabled())
      {
         log.trace("loadClass: " + className + ", bytes: " + bytes);
      }
      if(bytes != null)
      {
         for(int c = 0; c < bytes.length; c++)
         {
            addClass(bytes[c]);
         }
      }
      Class cl = lookupCachedClass(className);
      if(cl != null)
      {
         return cl;
      }
      cl = findLoadedClass(className);
      if(cl != null)
      {
         return cl;
      }
      cl = Class.forName(className, false, getSystemClassLoaderPrivate());
      if(cl != null)
      {
         return cl;
      }
      cl = Class.forName(className, false, getParent());
      if(cl != null)
      {
         return cl;
      }
      cl = loadFromNetwork(className);
      if(cl != null)
      {
         return cl;
      }
      throw new ClassNotFoundException("Could not load class " + className);
   }

   private void addClassResource(String name, byte buf[])
         throws IOException
   {
      // make sure we're OK on the reference queue
      performMaintenance();

      OutputStream out = null;
      File file = null;
      try
      {
         file = createTempFile("cbc", ".class", true);
         if(log.isTraceEnabled())
         {
            log.trace("adding resource at: " + name + " to file: " + file);
         }
         out = getFileOutputStream(file);
         out.write(buf);
         out.flush();
      }
      catch(java.io.IOException ex)
      {
         file = null;
         throw ex;
      }
      finally
      {
         if(out != null)
         {
            try
            {
               out.close();
            }
            catch(Exception ig)
            {
            }
            out = null;
         }
         if(file != null)
         {
            loadedResources.put(name, file);
         }
      }
   }

   public java.io.InputStream getResourceAsStream(String name)
   {
      // make sure we're OK on the reference queue
      performMaintenance();

      String denormalized = name.replace('/', '.').substring(0, name.length() - 6);
      java.io.File file = (java.io.File) loadedResources.get(denormalized);
      if(log.isTraceEnabled())
      {
         log.trace("getResourceAsStream =>" + denormalized + " = " + file);
      }
      if(file != null && fileExists(file))
      {
         try
         {
            InputStream is = getFileInputStream(file);
            return new java.io.BufferedInputStream(is);
         }
         catch(Exception ex)
         {
            log.debug("file doesn't exist", ex);
         }
      }
      return super.getResourceAsStream(name);
   }

   public Class addClass(ClassBytes classBytes)
         throws java.io.IOException
   {
      // make sure we're OK on the reference queue
      performMaintenance();

      Class cl = null;
      String name = classBytes.getClassName();
      if(loadedClasses.containsKey(name) == false)
      {
         byte buf[] = classBytes.getClassBytes();
         boolean array = ClassUtil.isArrayClass(name);
         String cn = (array) ? ClassUtil.getArrayClassPart(name) : name;
         if(log.isTraceEnabled())
         {
            log.trace("  add class: " + name + ", array?" + array + ", using as: " + cn);
         }
         cl = defineClass(cn, buf, 0, buf.length);
         resolveClass(cl);
         addClassResource(cn, buf);
         loadedClasses.put(cn, new MyRef(cn, cl));
      }
      return cl;
   }

   /**
    * lookup a cached class and return null if not found
    */
   private Class lookupCachedClass(String cn)
   {
      Class cl = null;
      MyRef ref = (MyRef) loadedClasses.get(cn);
      if(ref != null)
      {
         // make sure we've not gotten cleared
         cl = (Class) ref.get();
         if(cl == null)
         {
            // oops, we've been cleared
            clean(ref);
         }
      }
      return cl;
   }

   /**
    * Finds the specified class. This method should be overridden
    * by class loader implementations that follow the new delegation model
    * for loading classes, and will be called by the <code>loadClass</code>
    * method after checking the parent class loader for the requested class.
    * The default implementation throws <code>ClassNotFoundException</code>.
    *
    * @param name the name of the class
    * @return the resulting <code>Class</code> object
    * @throws ClassNotFoundException if the class could not be found
    * @since 1.2
    */
   protected Class findClass(String name) throws ClassNotFoundException
   {
      // make sure we're OK on the reference queue
      performMaintenance();

      boolean array = ClassUtil.isArrayClass(name);
      String cn = (array) ? ClassUtil.getArrayClassPart(name) : name;
      if(log.isTraceEnabled())
      {
         log.trace("++ loadClass: " + name + ", array?" + array + ", normalized: [" + cn + "]");
      }
      Class cl = lookupCachedClass(cn);
      // search the mapped classes first
      if(cl == null)
      {
         // search already loaded classes
         cl = findLoadedClass(cn);
      }
      if(cl != null)
      {
         if(array)
         {
            // we have to create an instance from the Class Part and return the
            // class ref from it
            Object obj = java.lang.reflect.Array.newInstance(cl, 1);
            return obj.getClass();
         }
         return cl;
      }

      cl = loadFromNetwork(cn);
      if(cl != null)
      {
         if(log.isTraceEnabled())
         {
            log.trace("Loaded " + cn + " can class is " + cl);
         }
         return cl;
      }


      if(log.isTraceEnabled())
      {
         log.trace("++ findClass: " + name + " not found, throwing ClassNotFoundException");
      }
      throw new ClassNotFoundException(name);
   }

   private Class loadFromNetwork(String className)
   {
      Class loadedClass = null;

      if(loaderClient != null)
      {
         String marshallerMethodName = "load_class";
         Map metadata = new HashMap();
         metadata.put("classname", className);

         try
         {
            if(!loaderClient.isConnected())
            {
                loaderClient.connect();
            }
            log.debug("attempting to load from network: " + className);
            Object obj = loaderClient.invoke(marshallerMethodName, metadata);
            log.debug("loaded from network: " + obj);

            if(obj != null)
            {
               if(obj instanceof ClassBytes)
               {
                  ClassBytes classBytes = (ClassBytes) obj;
                  if (classBytes.getClassBytes() != null)
                  {
                     loadedClass = addClass(classBytes);
                  }
                  else
                  {
                     log.debug("Can not load remote class bytes: server returned null class"); 
                  }
               }
               else
               {
                  log.error("Can not load remote class bytes.  Returned object (" + obj + ") is not ClassBytes.");
               }
            }
            else
            {
               log.error("Can not load remote class bytes.");
            }
         }
         catch(Throwable throwable)
         {
            log.error("Error loading remote class.", throwable);
         }
      }
      else
      {
         log.trace("Remoting Client for ClassByteClassLoader is null.  Can not load class remotely.");
      }

      return loadedClass;
   }
   
   static private File createTempFile(final String prefix, final String suffix, final boolean deleteOnExit) throws IOException
   {
      if (SecurityUtility.skipAccessControl())
      {
         File file =  File.createTempFile(prefix, suffix);
         if (deleteOnExit) file.deleteOnExit();
         return file;
      }
      
      try
      {
         return (File)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IOException
            {
               File file =  File.createTempFile(prefix, suffix);
               if (deleteOnExit) file.deleteOnExit();
               return file;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (IOException) e.getCause();
      }
   }
   
   static private boolean fileExists(final File file)
   {
      if (file == null)
         return false;
      
      if (SecurityUtility.skipAccessControl())
      {
         return file.exists();
      }

      return ((Boolean)AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            return new Boolean(file.exists());
         }
      })).booleanValue();
   }
   
   static private FileInputStream getFileInputStream(final File file) throws FileNotFoundException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return new FileInputStream(file);
      }
      
      try
      {
         return (FileInputStream)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws FileNotFoundException
            {
               return new FileInputStream(file);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (FileNotFoundException) e.getCause();
      }
   }
   
   static private FileOutputStream getFileOutputStream(final File file)
   throws FileNotFoundException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return new FileOutputStream(file);
      }
      
      try
      {
         return (FileOutputStream)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws FileNotFoundException
            {
               return new FileOutputStream(file);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (FileNotFoundException) e.getCause();
      }
   }
   
   static private ClassLoader getSystemClassLoaderPrivate()
   {
      if (SecurityUtility.skipAccessControl())
      {
         return ClassLoader.getSystemClassLoader();
      }

      return (ClassLoader)AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            return ClassLoader.getSystemClassLoader();
         }
      });
   }
}
