/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.test.remoting.marshall.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.marshal.serializable.SerializableUnMarshaller;

/**
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version 
 * <p>
 * Copyright Mar 24, 2009
 * </p>
 */
public class ConfigTestUnmarshaller extends SerializableUnMarshaller
{
   protected static Logger log = Logger.getLogger(ConfigTestUnmarshaller.class);
   private static final long serialVersionUID = 1L;
   public static volatile int cloned;
   public static volatile boolean read;

   public UnMarshaller cloneUnMarshaller() throws CloneNotSupportedException
   {
      cloned++;
      log.info("cloned ConfigTestUnmarshaller");
//      log.info("cloned ConfigTestUnmarshaller", new Exception());
      ConfigTestUnmarshaller unmarshaller = new ConfigTestUnmarshaller();
      unmarshaller.setClassLoader(this.customClassLoader);
      return unmarshaller;
   }
   
   public Object read(InputStream inputStream, Map metadata, int version) throws IOException, ClassNotFoundException
   {
      Object o = super.read(inputStream, metadata, version);
      if (!(o instanceof Wrapper))
      {
         throw new IOException("expected Wrapper");
      }
      log.info(this + "read Wrapper");
      read = true;
      return ((Wrapper)o).wrappee;
   }
   
   public static boolean ok(boolean b, int count)
   {
      log.info("read: " + read + ", cloned: " + cloned);
      return read == b && cloned == count;
   }
   
   public static void reset()
   {
      cloned = 0;
      read = false;
   }
}

