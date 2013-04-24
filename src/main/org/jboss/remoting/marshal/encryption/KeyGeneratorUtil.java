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
package org.jboss.remoting.marshal.encryption;

import org.jboss.logging.Logger;

import javax.crypto.KeyGenerator;
import java.io.FileOutputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.security.Key;

//$Id: KeyGeneratorUtil.java 1376 2006-08-18 17:49:04Z telrod $

/**
 *  Generates keys
 *  @author <a href="mailto:Anil.Saldhana@jboss.org">Anil Saldhana</a>
 *  @since  Aug 14, 2006
 *  @version $Revision: 1376 $
 */
public class KeyGeneratorUtil
{
   protected final static Logger log = Logger.getLogger(KeyGeneratorUtil.class);

   public void genKeys() throws Exception
   {
      getKey("DES");
      getKey("DESede");
      getKey("AES");
      getKey("RC4");
      getKey("Blowfish");
   }

   private Key getKey(String algo)
   {
     Key key = null;
     try
     {
        KeyGenerator gen = KeyGenerator.getInstance(algo);
        key = gen.generateKey();
        serializeToFile(key,algo);
     }
     catch (Exception e)
     {
        log.error(e.getMessage(), e);
     }
     return key;
   }

   private void serializeToFile(Key key, String algo) throws Exception
   {
      ObjectOutput out = new ObjectOutputStream(new FileOutputStream(algo+".key"));
      out.writeObject(key);
      out.close();
   }

   public static void main(String[] args)
   {
      KeyGeneratorUtil u = new KeyGeneratorUtil();
      try
      {
         u.genKeys();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }
}
