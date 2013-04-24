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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Set;
import org.jboss.logging.Logger;


/**
 * ClassUtil is a set of generic class utlities.
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 * @version $Revision: 4145 $
 */
public class ClassUtil
{
   protected final static Logger log = Logger.getLogger(ClassUtil.class);

   public static Object deserialize(ClassBytes cb, ClassLoader cl)
         throws IOException, ClassNotFoundException
   {
      if(cb.getClassBytes() == null)
      {
         return null;
      }
      java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(cb.getClassBytes());
      java.io.ObjectInputStream ois = new ObjectInputStreamWithClassLoader(bis, cl);
      Object result = ois.readObject();
      bis = null;
      ois = null;
      return result;
   }

   public static Object deserialize(byte buf[])
         throws IOException, ClassNotFoundException
   {
      ClassLoader cl = (ClassLoader) AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader == null) loader = ClassUtil.class.getClassLoader();
            return loader;
         }
      });
      
      return deserialize(buf, cl);
   }

   public static Object deserialize(byte buf[], ClassLoader cl)
         throws IOException, ClassNotFoundException
   {
      if(buf == null)
      {
         return null;
      }
      java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(buf);
      java.io.ObjectInputStream ois = new ObjectInputStreamWithClassLoader(bis, cl);
      Object result = ois.readObject();
      bis = null;
      ois = null;
      return result;
   }

   public static byte[] serialize(Object obj)
         throws java.io.IOException
   {
      java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
      java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(bos);
      oos.writeObject(obj);
      oos.flush();
      bos.flush();
      byte buf[] = bos.toByteArray();
      bos = null;
      oos = null;
      return buf;
   }

   public static boolean isArrayClass(String className)
   {
      return (className.startsWith("[L") && className.endsWith(";"));
   }

   public static String getArrayClassPart(String className)
   {
      String cn = className;
      int i = className.indexOf("[L");
      if(i > -1)
      {
         cn = className.substring(i + 2, className.length() - 1);
      }
      return cn;
   }

   public static String getPackageName(Class cl)
   {
      String n = cl.getName();
      int i = n.lastIndexOf(".");
      return (i > -1) ? n.substring(0, i) : n;
   }

   public static String getShortClassName(Class cl)
   {
      String n = cl.getName();
      int i = n.lastIndexOf(".");
      return (i > -1) ? n.substring(i + 1) : n;
   }

   /**
    * given a class, recurse its dependency graph and find all its implemented interfaces
    *
    * @param clazz
    * @return array of interfaces
    */
   public static Class[] getInterfacesFor(Class clazz)
   {
      // use a set to eliminate duplicates, since you'll get a
      // java.lang.ClassFormatError: $Proxy8 (Repetitive interface name))
      Set set = new HashSet();
      addInterfaces(set, clazz);
      return (Class[]) set.toArray(new Class[set.size()]);
   }

   private static void addInterfaces(Set list, Class clazz)
   {
      if(clazz != null && clazz != Object.class)
      {
         if(clazz.isInterface() && list.contains(clazz) == false)
         {
            list.add(clazz);
         }
         Class interfaces[] = clazz.getInterfaces();
         if(interfaces != null && interfaces.length > 0)
         {
            for(int c = 0; c < interfaces.length; c++)
            {
               Class interfaceClass = interfaces[c];
               if(list.contains(interfaceClass) == false)
               {
                  list.add(interfaceClass);
               }
               addInterfaces(list, interfaceClass);
            }
         }
         addInterfaces(list, clazz.getSuperclass());
      }
   }

   /**
    * method is called to retrieve a byte array of a Class for a given class name
    *
    * @param className
    * @return
    */
   public static byte[] getClassBytes(String className, ClassLoader classbyteloader)
   {
      String cn = null;
      if(isArrayClass(className))
      {
         // if requesting an array, of course, that would be found in our class path, so we
         // need to strip the class data and just return the class part, the other side
         // will properly load the class as an array
         cn = getArrayClassPart(className).replace('.', '/') + ".class";
      }
      else
      {
         cn = className.replace('.', '/') + ".class";
      }
      if(log.isTraceEnabled())
      {
         log.trace("trying to load class: " + className + " from path: " + cn);
      }
      InputStream in = null;
      ClassLoader cl = classbyteloader;

      if(cl == null)
      {
         cl = (ClassLoader) AccessController.doPrivileged( new PrivilegedAction()
         {
            public Object run()
            {
               return ClassLoader.getSystemClassLoader();
            }
         });
      }
      if(cl != null)
      {
         try
         {
            final ClassLoader fcl = cl;
            final String fcn = cn;
            in = (InputStream) AccessController.doPrivileged( new PrivilegedAction()
            {
               public Object run()
               {
                  return fcl.getResourceAsStream(fcn);
               }
            });
         }
         catch (Exception e)
         {
            log.error("error getting resource " + cn, e);
         }
         if(in != null)
         {
            if(log.isTraceEnabled())
            {
               log.trace("looking for classes at: " + cl);
            }
            try
            {
               byte data[] = read(in);
               if(log.isTraceEnabled())
               {
                  log.trace("found class at classloader: " + cl);
               }
               return data;
            }
            catch(IOException io)
            {
            }
            finally
            {
               if(in != null)
               {
                  try
                  {
                     in.close();
                  }
                  catch(Exception ig)
                  {
                  }
                  in = null;
               }
            }
         }
      }
      return null;
   }

   /**
    * simple utility method for reading bytes from an input stream
    *
    * @param in
    * @return
    * @throws IOException
    */
   protected static byte[] read(InputStream in)
         throws IOException
   {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte buf[] = new byte[4096];
      while(true)
      {
         int c = in.read(buf);
         if(c < 0)
         {
            break;
         }
         out.write(buf, 0, c);
      }
      return out.toByteArray();
   }

}
