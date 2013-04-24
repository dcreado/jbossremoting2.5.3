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

package org.jboss.test.remoting.classloader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import org.jboss.logging.Logger;
import org.jboss.remoting.invocation.NameBasedInvocation;
import org.jboss.test.remoting.ComplexReturn;
import org.jboss.test.remoting.transport.mock.MockTest;

import junit.framework.TestCase;


/**
 * This is a copy of org.jboss.test.remoting.transport.socket.InvokerClientTest but
 * uses custom classloader (and reflection) to load and make the remoting calls.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class InvokerClientTest extends TestCase
{
   private int port = 8081;
   private String transport = "socket";

   private static final Logger log = Logger.getLogger(InvokerClientTest.class);

   public void testArrayReturn() throws Throwable
   {

      String[] classUrls = new String[]{"org.jboss.util.threadpool.ThreadPool",
            "javax.servlet.ServletInputStream",
            "org.apache.coyote.Adapter",
            "org.jboss.serial.io.JBossObjectOutputStream",
            "EDU.oswego.cs.dl.util.concurrent.SynchronizedLong",
            "org.jboss.logging.Logger"};
      URL[] urls = getLibUrls(classUrls);
      URLClassLoader urlclassloader = new URLClassLoader(urls, null);
      TestClassLoader classloader = new TestClassLoader(urlclassloader);

      /*
      Class invokerLocator = loadClass("org.jboss.remoting.InvokerLocator", classloader);
      Constructor invokerLocatorConstructor = invokerLocator.getConstructor(new Class[] { String.class});
      Object invokerLocatorInstance = invokerLocatorConstructor.newInstance(new Object[] { transport + "://localhost:" + port});

      loadClass("org.jboss.remoting.Invoker", classloader);
      loadClass("org.jboss.remoting.loading.ClassByteClassLoader", classloader);
      loadClass("org.jboss.remoting.transport.ClientInvoker", classloader);
      loadClass("org.jboss.remoting.AbstractInvoker", classloader);
      loadClass("org.jboss.remoting.transport.local.LocalClientInvoker", classloader);
      loadClass("org.jboss.remoting.InvokerRegistry", classloader);
      loadClass("org.jboss.remoting.InvalidConfigurationException", classloader);
      loadClass("org.jboss.remoting.RemoteClientInvoker", classloader);
      loadClass("org.jboss.remoting.transport.socket.SocketClientInvoker", classloader);
      Class clientClz = loadClass("org.jboss.remoting.Client", classloader);
      Constructor clientConstructor = clientClz.getConstructor(new Class[] {invokerLocator, String.class});
      Object clientInstance = clientConstructor.newInstance(new Object[] {invokerLocatorInstance, "mock"});
      */

      Class invokerLocator = Class.forName("org.jboss.remoting.InvokerLocator", false, classloader);
      Constructor invokerLocatorConstructor = invokerLocator.getConstructor(new Class[]{String.class});
      Object invokerLocatorInstance = invokerLocatorConstructor.newInstance(new Object[]{transport + "://localhost:" + port});

      Class clientClz = Class.forName("org.jboss.remoting.Client", false, classloader);
      Constructor clientConstructor = clientClz.getConstructor(new Class[]{invokerLocator, String.class});
      Object clientInstance = clientConstructor.newInstance(new Object[]{invokerLocatorInstance, "mock"});

      ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
      System.out.println("context classloader: " + contextClassLoader);
      System.out.println("test classloader: " + classloader);

      Class.forName("org.jboss.test.remoting.ComplexReturn", false, contextClassLoader);


      Method connectMethod = clientClz.getMethod("connect", new Class[]{});
      connectMethod.invoke(clientInstance, null);

      // simple invoke, should return bar
      Object ret = null;

      NameBasedInvocation nbinv = new NameBasedInvocation("testComplexReturn",
                                                          new Object[]{null},
                                                          new String[]{String.class.getName()});

      Method invokeMethod = clientClz.getMethod("invoke", new Class[]{Object.class, Map.class});
      ret = invokeMethod.invoke(clientInstance, new Object[]{nbinv, null});


      ComplexReturn complexRet = (ComplexReturn) ret;
      MockTest[] mockTests = complexRet.getMockTests();
      assertTrue("ComplexReturn's array should contain 2 items",
                 2 == mockTests.length);
      if(2 == mockTests.length)
      {
         log.debug("PASS");
      }
      else
      {
         log.debug("FAILED - testArrayReturn1");
      }

      for(int x = 0; x < mockTests.length; x++)
      {
         System.err.println(mockTests[x]);
         MockTest test = mockTests[x];
         assertNotNull("MockTest should not be null", test);
         if(test != null)
         {
            log.debug("PASS");
         }
         else
         {
            log.debug("FAILED - testArrayReturn2");
         }

      }

      nbinv = new NameBasedInvocation("testThrowException",
                                      new Object[]{null},
                                      new String[]{String.class.getName()});

      invokeMethod = clientClz.getMethod("invoke", new Class[]{Object.class, Map.class});
      try
      {
         ret = invokeMethod.invoke(clientInstance, new Object[]{nbinv, null});
         assertTrue("Should have thrown exception and not reached this assert.", false);
      }
      catch(Exception e)
      {
         assertTrue("Got exception thrown as expected", true);
      }

      invokeMethod = clientClz.getMethod("disconnect", new Class[]{});
      invokeMethod.invoke(clientInstance, new Object[]{});

   }

   private URL[] getLibUrls(String[] classNames) throws Exception
   {
      URL[] urls = new URL[classNames.length + 1];

      String url = null;
      int i = -1;
      int x = 0;
      for(; x < classNames.length; x++)
      {
         String className = classNames[x];

         int index = className.lastIndexOf('.');
         String fileName = className.substring(index + 1);

         Class clz = log.getClass().forName(className);

         URL fileURL = clz.getResource(fileName + ".class");
         if(fileURL == null)
         {
            throw new Exception(fileName + ".class file not found");
         }

         String fullURL = fileURL.getPath();
         i = fullURL.indexOf("!");
         url = fullURL.substring(0, i);

         urls[x] = new URL(url);
      }

      String fileSep = System.getProperty("file.separator");
      i = url.indexOf("lib");
      String remotingJarPath = url.substring(0, i);
      remotingJarPath = remotingJarPath + "output" + fileSep + "lib" + fileSep + "jboss-remoting.jar";
      urls[x] = new URL(remotingJarPath);

      return urls;
   }

   private static Class loadClass(String className, TestClassLoader classLoader) throws Exception
   {
      int index = className.lastIndexOf('.');
      String fileName = className.substring(index + 1);

      Class clz = log.getClass().forName(className);

      URL fileURL = clz.getResource(fileName + ".class");
      if(fileURL == null)
      {
         throw new Exception(fileName + ".class file not found");
      }
      File testFile = new File(fileURL.getFile());
      FileInputStream fileInput = new FileInputStream(testFile);

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte buf[] = new byte[4096];
      while(true)
      {
         int c = fileInput.read(buf);
         if(c < 0)
         {
            break;
         }
         out.write(buf, 0, c);
      }
      byte[] bytes = out.toByteArray();

      return classLoader.loadClass(className, bytes);
   }


   public static class TestClassLoader extends ClassLoader
   {

      /**
       * Creates a new class loader using the specified parent class loader for
       * delegation.
       * <p/>
       * <p> If there is a security manager, its {@link
       * SecurityManager#checkCreateClassLoader()
       * <tt>checkCreateClassLoader</tt>} method is invoked.  This may result in
       * a security exception.  </p>
       *
       * @param parent The parent class loader
       * @throws SecurityException If a security manager exists and its
       *                           <tt>checkCreateClassLoader</tt> method doesn't allow creation
       *                           of a new class loader.
       * @since 1.2
       */
      protected TestClassLoader(ClassLoader parent)
      {
         super(parent);
      }

      protected Class loadClass(String className, byte[] classBytes) throws ClassNotFoundException
      {
         Class c = defineClass(className, classBytes, 0, classBytes.length);
         resolveClass(c);
         return Class.forName(className, false, this);
      }
   }

}
