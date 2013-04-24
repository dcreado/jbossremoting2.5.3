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
import java.io.OutputStream;

import org.apache.log4j.Logger;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.serializable.SerializableMarshaller;

/**
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version 
 * <p>
 * Copyright Mar 24, 2009
 * </p>
 */
public class ConfigTestMarshaller extends SerializableMarshaller
{
   protected static Logger log = Logger.getLogger(ConfigTestMarshaller.class);
   private static final long serialVersionUID = 1L;
   private static volatile int cloned;
//   private static volatile int wrote;
   private static volatile IntHolder wrote = new IntHolder(0);

   public void write(Object dataObject, OutputStream output, int version) throws IOException
   {
      log.info(this + "writing Wrapper");
      super.write(new Wrapper(dataObject), output, version);
//      wrote++;
      wrote.increment();
      log.info("wrote: " + wrote + ", cloned: " + cloned);
   }
   
   public Marshaller cloneMarshaller() throws CloneNotSupportedException
   {
      cloned++;
      log.info("cloned ConfigTestMarshaller: wrote: " + wrote + ", cloned: " + cloned );
//      log.info("cloned ConfigTestMarshaller", new Exception());
      return new ConfigTestMarshaller();
   }
   
   public static boolean ok(boolean b, int count)
   {
      log.info("wrote: " + wrote + ", cloned: " + cloned);
//      return (b ? wrote > 0 : wrote == 0) && cloned == count;
      int w = wrote.getI();
      return (b ? w > 0 : w == 0);
   }
   
   public static void reset()
   {
      cloned = 0;
//      wrote = 0;
      wrote.setI(0);
      log.info("reset(): wrote: " + wrote + ", cloned: " + cloned);
   }
   
   static class IntHolder
   {
      int i;
      
      IntHolder(int i)
      {
         this.i = i;
      }
      public int getI()
      {
         return i;
      }
      public void setI(int i)
      {
         this.i = i;
         log.info("setting i", new Exception("setting i"));
      }
      public void increment()
      {
         i++;
         log.info("incrementing i", new Exception("incrementing i")); 
      }
      public String toString()
      {
         return Integer.toString(i);
      }
   }
}

