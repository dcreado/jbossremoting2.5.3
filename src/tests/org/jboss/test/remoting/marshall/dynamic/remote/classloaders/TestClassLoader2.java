
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
package org.jboss.test.remoting.marshall.dynamic.remote.classloaders;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import org.apache.log4j.Logger;

/**
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Aug 8, 2008
 * </p>
 */
public class TestClassLoader2 extends ClassLoader
{
   public boolean queriedForTarget;
   
   private static Logger log = Logger.getLogger(TestClassLoader2.class);
   
   private String jarFileName;
   private String targetClassName;
   private String targetClassFileName;
   
   
   public static void main(String[] args)
   {
      String jarFileName = "C:/cygwin/home/rsigal/workspace/JBossRemoting-2.x/output/lib/jboss-remoting-loading-tests.jar";
      String className = "org.jboss.test.remoting.marshall.dynamic.remote.classloaders.ResponseImpl";
      ClassLoader cl = TestClassLoader2.class.getClassLoader();
      TestClassLoader2 tcl2 = new TestClassLoader2(cl, jarFileName, className);
      try
      {
         Class c = tcl2.loadClass(className);
         Object o = c.newInstance();
         log.info("new object: " + o);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }
   
   
   public TestClassLoader2(ClassLoader parent, String jarFileName, String targetClassName)
   {
      super(parent);
      this.jarFileName = jarFileName;
      this.targetClassName = targetClassName;
      this.targetClassFileName = targetClassName.replace('.', '/') + ".class";
   }
   
   
   public InputStream getResourceAsStream(String name) 
   {
      try
      {
         log.info(this + " queried for resource InputStream: " + name);
         if (targetClassFileName.equals(name))
         {
            log.info(this + " looking for resource InputStream: " + name);
            queriedForTarget = true;
            byte[] bytecode = loadByteCodeFromClassFileName(name);
            log.info(this + " returning resource InputStream: " + name);
            return new ByteArrayInputStream(bytecode);
         }
         else
         {
            return super.getResourceAsStream(name);
         }
      }
      catch (Exception e)
      {
         log.error(this + " unable to find resource: " + name);
         return null;
      }
   }
   
   
   protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException
   {
      try
      {
         log.info(this + " queried for class: " + name);
         if (targetClassName.equals(name))
         {
            log.info(this + " loading class: " + name);
            queriedForTarget = true;
            byte[] bytes = loadByteCodeFromClassName(name);
            Class c = defineClass(name, bytes, 0, bytes.length);
            if (resolve)
            {
               resolveClass(c);
            }
            return c;
         }
         else
         {
            return super.loadClass(name, resolve);
         }
      }
      catch (IOException e)
      {
         throw new ClassNotFoundException(name);
      }
   }
   
   
   private byte[] loadByteCodeFromClassName(String classname)
   throws ClassNotFoundException, IOException
   {
      String classFileName = classname.replace('.', '/') + ".class";
      return loadByteCodeFromClassFileName(classFileName);
   }
   
   
   private byte[] loadByteCodeFromClassFileName(String classFileName)
   throws ClassNotFoundException, IOException
   {
      File file = new File(jarFileName);
      JarInputStream jis = new JarInputStream(new FileInputStream(file));
      ZipEntry entry = jis.getNextEntry();
      
      try
      {
         while (entry != null)
         {
            log.info("name: " + entry.getName());
            if (classFileName.equals(entry.getName()))
               break;
            entry = jis.getNextEntry();
         }
         log.info("size: " + entry.getSize());
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         byte[] tmp = new byte[1024];
         int read = 0;
         while( (read = jis.read(tmp)) > 0 )
         {
            baos.write(tmp, 0, read);
         }
         byte[] bytecode = baos.toByteArray();
         return bytecode;
      }
      finally
      {
         if( jis != null )
            jis.close();
      }
   }
}
