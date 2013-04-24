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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;

import org.jboss.logging.Logger;
import org.jboss.remoting.util.SecurityUtility;


/**
 * ObjectInputStreamWithClassLoader
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 * @version $Revision: 5003 $
 */
public class ObjectInputStreamWithClassLoader extends ObjectInputStream
{

   protected static Method clearMethod;

   protected static final Logger log = Logger.getLogger(ObjectInputStreamWithClassLoader.class);



   static
   {
      try
      {
         clearMethod = getDeclaredMethod(ObjectInputStream.class, "clear", new Class[]{});
         
      } catch (SecurityException e) {
         log.error(e.getMessage(), e);
      } catch (NoSuchMethodException e) {
         log.error(e.getMessage(), e);
      }
   }

   private ClassLoader cl;

   // EJBTHREE-440
   /** table mapping primitive type names to corresponding class objects */
   private static final HashMap primClasses = new HashMap(8, 1.0F);
   static {
      primClasses.put("boolean", boolean.class);
      primClasses.put("byte", byte.class);
      primClasses.put("char", char.class);
      primClasses.put("short", short.class);
      primClasses.put("int", int.class);
      primClasses.put("long", long.class);
      primClasses.put("float", float.class);
      primClasses.put("double", double.class);
      primClasses.put("void", void.class);
   }

   /**
    * Create an ObjectInputStream that reads from the specified InputStream.
    * The stream header containing the magic number and version number
    * are read from the stream and verified. This method will block
    * until the corresponding ObjectOutputStream has written and flushed the
    * header.
    *
    * @param in the underlying <code>InputStream</code> from which to read
    * @throws java.io.StreamCorruptedException
    *                             The version or magic number are
    *                             incorrect.
    * @throws java.io.IOException An exception occurred in the underlying stream.
    */
   public ObjectInputStreamWithClassLoader(java.io.InputStream in, ClassLoader cl)
         throws IOException, StreamCorruptedException
   {
      super(in);
      this.cl = cl;
   }

   /**
    * Set the classloader that the stream will used when deserializing class.  This will
    * allow plugging in of classloaders based on invocation context.
    *
    * @param cl
    */
   public void setClassLoader(ClassLoader cl)
   {
      this.cl = cl;
   }

   /**
    * Gets the pluggable classloader that will be used for classloading when deserializing
    * objects.
    *
    * @return
    */
   public ClassLoader getClassLoader()
   {
      return cl;
   }
   
   public void clearCache()
   {
       try
       {
           clearMethod.invoke(this, new Object[]{});
       }
       catch (Throwable e)
       {
           log.error(e.getMessage(), e);
       }
       
   }

   /**
    * Load the local class equivalent of the specified stream class description.
    * <p/>
    * Subclasses may implement this method to allow classes to be
    * fetched from an alternate source.
    * <p/>
    * The corresponding method in ObjectOutputStream is
    * annotateClass.  This method will be invoked only once for each
    * unique class in the stream.  This method can be implemented by
    * subclasses to use an alternate loading mechanism but must
    * return a Class object.  Once returned, the serialVersionUID of the
    * class is compared to the serialVersionUID of the serialized class.
    * If there is a mismatch, the deserialization fails and an exception
    * is raised. <p>
    * <p/>
    * By default the class name is resolved relative to the class
    * that called readObject. <p>
    * <p/>
    * Will use the classloader explicitly set if it exists.  If it does not exist,
    * will used its default classloader (that loader this instance).<p>
    *
    * @param v an instance of class ObjectStreamClass
    * @return a Class object corresponding to <code>v</code>
    * @throws java.io.IOException Any of the usual Input/Output exceptions.
    * @throws java.lang.ClassNotFoundException
    *                             If class of
    *                             a serialized object cannot be found.
    */
   protected Class resolveClass(java.io.ObjectStreamClass v)
         throws java.io.IOException, ClassNotFoundException
   {
      if(cl == null)
      {
         return super.resolveClass(v);
      }
      else
      {
         // EJBTHREE-440 & JBREM-508
         try
         {
            return Class.forName(v.getName(), false, cl);
         }
         catch(ClassNotFoundException ex)
         {
            Class cl = (Class) primClasses.get(v.getName());
            if (cl != null) {
               return cl;
            } else {
               throw ex;
            }
         }
      }
   }

   /**
    * Returns a proxy class that implements the interfaces named in a
    * proxy class descriptor; subclasses may implement this method to
    * read custom data from the stream along with the descriptors for
    * dynamic proxy classes, allowing them to use an alternate loading
    * mechanism for the interfaces and the proxy class.
    * <p/>
    * <p>This method is called exactly once for each unique proxy class
    * descriptor in the stream.
    * <p/>
    * <p>The corresponding method in <code>ObjectOutputStream</code> is
    * <code>annotateProxyClass</code>.  For a given subclass of
    * <code>ObjectInputStream</code> that overrides this method, the
    * <code>annotateProxyClass</code> method in the corresponding
    * subclass of <code>ObjectOutputStream</code> must write any data or
    * objects read by this method.
    * <p/>
    * <p>The default implementation of this method in
    * <code>ObjectInputStream</code> returns the result of calling
    * <code>Proxy.getProxyClass</code> with the list of
    * <code>Class</code> objects for the interfaces that are named in
    * the <code>interfaces</code> parameter.  The <code>Class</code>
    * object for each interface name <code>i</code> is the value
    * returned by calling
    * <pre>
    *     Class.forName(i, false, loader)
    * </pre>
    * where <code>loader</code> is that of the first non-null class
    * loader up the execution stack, or <code>null</code> if no non-null
    * class loaders are on the stack (the same class loader choice used
    * by the <code>resolveClass</code> method).  This same value of
    * <code>loader</code> is also the class loader passed to
    * <code>Proxy.getProxyClass</code>.  If <code>Proxy.getProxyClass</code>
    * throws an <code>IllegalArgumentException</code>,
    * <code>resolveProxyClass</code> will throw a
    * <code>ClassNotFoundException</code> containing the
    * <code>IllegalArgumentException</code>.
    *
    * @return a proxy class for the specified interfaces
    * @param   interfaces the list of interface names that were
    * deserialized in the proxy class descriptor
    * @throws java.io.IOException any exception thrown by the underlying
    * <code>InputStream</code>
    * @throws java.lang.ClassNotFoundException if the proxy class or any of the
    * named interfaces could not be found
    * @since 1.3
    * @see java.io.ObjectOutputStream#annotateProxyClass(java.lang.Class)
    */
   protected Class resolveProxyClass(String[] interfaces)
         throws java.io.IOException, ClassNotFoundException
   {
      if(cl == null)
      {
         return super.resolveProxyClass(interfaces);
      }
      else
      {
         Class[] classObjs = new Class[interfaces.length];
         for(int i = 0; i < interfaces.length; i++)
         {
            classObjs[i] = Class.forName(interfaces[i], false, cl);
         }
         try
         {
            return java.lang.reflect.Proxy.getProxyClass(cl, classObjs);
         }
         catch(IllegalArgumentException e)
         {
            throw new ClassNotFoundException(null, e);
         }
      }
   }
   
   static private Method getDeclaredMethod(final Class c, final String name, final Class[] parameterTypes)
   throws NoSuchMethodException
   {
      if (SecurityUtility.skipAccessControl())
      {
         Method m = c.getDeclaredMethod(name, parameterTypes);
         m.setAccessible(true);
         return m;
      }

      try
      {
         return (Method) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws NoSuchMethodException
            {
               Method m = c.getDeclaredMethod(name, parameterTypes);
               m.setAccessible(true);
               return m;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (NoSuchMethodException) e.getCause();
      }
   }
}