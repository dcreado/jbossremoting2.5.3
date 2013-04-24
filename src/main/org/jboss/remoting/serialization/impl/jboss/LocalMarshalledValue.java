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


package org.jboss.remoting.serialization.impl.jboss;

import org.jboss.logging.Logger;
import org.jboss.serial.objectmetamodel.DataContainer;
import org.jboss.serial.objectmetamodel.safecloning.SafeCloningRepository;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Does lazy serialization based on JBossSerialization.
 * I'm using JbossSerialization internal's API here, because DataContainer is a repository of final variables,
 * and in case we are only using this MarshalledValue to cross ClassLoader isolations, we don't need to convert
 * the repository to a byteArray.
 * <p/>
 * $Id: LocalMarshalledValue.java 3845 2008-04-02 04:12:05Z ron.sigal@jboss.com $
 *
 * @author <a href="mailto:tclebert.suconic@jboss.com">Clebert Suconic</a>
 */
public class LocalMarshalledValue extends org.jboss.remoting.serialization.RemotingMarshalledValue implements Externalizable
{

   protected static final Logger log = Logger.getLogger(JBossSerializationManager.class);

   DataContainer container;
   private static final long serialVersionUID = 6996297171147626666L;

   public LocalMarshalledValue()
   {
   }

   public LocalMarshalledValue(Object obj) throws IOException
   {
      container = new DataContainer(false);
      ObjectOutput output = container.getOutput();
      output.writeObject(obj);
      output.flush();
      container.flush();
   }

   public LocalMarshalledValue(Object obj, SafeCloningRepository safeToReuse) throws IOException
   {
      container = new DataContainer(null, null, safeToReuse, false);
      ObjectOutput output = container.getOutput();
      output.writeObject(obj);
      output.flush();
      container.flush();
   }

   /**
    * The object has to be unserialized only when the first get is executed.
    */
   public Object get() throws IOException, ClassNotFoundException
   {
      try
      {
         ClassLoader tcl = (ClassLoader) AccessController.doPrivileged( new PrivilegedAction()
         {
            public Object run()
            {
               return Thread.currentThread().getContextClassLoader();
            }
         });
         container.getCache().setLoader(tcl);
         return container.getInput().readObject();
      }
      catch(RuntimeException e)
      {
         log.debug(e, e);
         throw e;
      }
   }

   public void writeExternal(ObjectOutput out) throws IOException
   {
	  log.warn("LocalmarshalledValue writeExternal is deprecated. This version is best used on call-by-value operations");
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
      BufferedOutputStream buffOut = new BufferedOutputStream(byteOut);
      DataOutputStream dataOut = new DataOutputStream(buffOut);
      container.saveData(dataOut);
      dataOut.flush();
      byte[] arrayBytes = byteOut.toByteArray();
      out.writeInt(arrayBytes.length);
      out.write(arrayBytes);
      out.flush();
   }

   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      log.warn("LocalmarshalledValue readExternal is deprecated. This version is best used on call-by-value operations");
      byte[] bytes = new byte[in.readInt()];
      in.readFully(bytes);
      ByteArrayInputStream byteInput = new ByteArrayInputStream(bytes);
      DataInputStream input = new DataInputStream(byteInput);
      container = new DataContainer(false);
      container.loadData(input);
   }
}
