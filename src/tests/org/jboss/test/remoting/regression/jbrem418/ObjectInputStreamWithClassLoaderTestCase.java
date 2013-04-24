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

package org.jboss.test.remoting.regression.jbrem418;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import junit.framework.TestCase;

import org.jboss.remoting.loading.ObjectInputStreamWithClassLoader;

/**
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: 3872 $
 */
public class ObjectInputStreamWithClassLoaderTestCase extends TestCase
{
   public static void main(String[] args)
   {
      junit.textui.TestRunner.run(ObjectInputStreamWithClassLoaderTestCase.class);
   }

   public void testIntPrimitive() throws Exception
   {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream out = new ObjectOutputStream(baos);
      out.writeObject(int.class);
      out.flush();
      out.close();
      byte data[] = baos.toByteArray();
      
      ClassLoader cl = (ClassLoader)AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            return new ClassLoader() {};
         }
      });
      
      ByteArrayInputStream bais = new ByteArrayInputStream(data);
      ObjectInputStreamWithClassLoader in = new ObjectInputStreamWithClassLoader(bais, cl);
      in.readObject();
      in.close();
   }
}
