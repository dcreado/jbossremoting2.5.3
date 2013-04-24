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

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import org.jboss.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.security.AccessController;
import java.security.Key;
import java.security.PrivilegedAction;
import java.util.Map;

//$Id: EncryptionManager.java 3836 2008-04-02 03:56:30Z ron.sigal@jboss.com $

/**
 *  Manager that deals with the generation of the Cipher
 *  Mode:
 *       ECB: Electronic Codebook Mode (NIST FIPS PUB 81)
 *       CBC: Cipher Block Chaining Mode(NIST FIPS PUB 81)
 *       PCBC: Plaintext Cipher Block Chaining (Kerberos)
 *       CFB: Cipher Feedback Mode (NIST FIPS PUB 81)
 *       OFB: Output Feedback Mode (NIST FIPS PUB 81)
 *  Padding:
 *       NoPadding: No padding.
         PKCS5Padding: RSA, "PKCS #5: Password-Based Encryption Standard,"
                       version 1.5, Nov 1993.
 *  @author <a href="mailto:Anil.Saldhana@jboss.org">Anil Saldhana</a>
 *  @since  Aug 11, 2006
 *  @version $Revision: 3836 $
 */
public class EncryptionManager
{
   private static Logger log = Logger.getLogger(EncryptionManager.class);
   private static Map keys =  new ConcurrentHashMap();

   private static byte[] salt8 = {
      (byte)0x7e, (byte)0xee, (byte)0xc8, (byte)0xc7,
      (byte)0x99, (byte)0x73, (byte)0x21, (byte)0x8c};

   private static byte[] salt16 = {
      (byte)0x7e, (byte)0xee, (byte)0xc8, (byte)0xc7,
      (byte)0x99, (byte)0x73, (byte)0x21, (byte)0x8c,
      (byte)0x7e, (byte)0xee, (byte)0xc8, (byte)0xc7,
      (byte)0x99, (byte)0x73, (byte)0x21, (byte)0x8c};

   private static IvParameterSpec iv8 = new IvParameterSpec(salt8);

   private static IvParameterSpec iv16 = new IvParameterSpec(salt16);

   
   public static final String TRIPLEDES = "DESede";
   public static final String DES = "DES";
   public static final String AES = "AES";
   public static final String BLOWFISH = "Blowfish";
   public static final String RC4 = "RC4"; 

   public static final String DEFAULT_CIPHER_ALGORITHM = DES;

   static
   {
      //Generate Keys for the common algorithms
      try
      {
         keys.put("AES", loadKey(AES));
         keys.put("DES", loadKey(DES));
         keys.put("DESede", loadKey(TRIPLEDES));
         keys.put("Blowfish", loadKey(BLOWFISH));
         keys.put("RC4", loadKey(RC4));
      }
      catch (Exception e)
      {
         if(log.isTraceEnabled())
            log.trace("Exception in loading key",e);
      }
   }

   /**
    * Generate a Cipher
    * @param mode Cipher.ENCRYPT_MODE or Cipher.DECRYPT_MODE (Wrap/Unwrap not supported)
    * @param algo Cipher Algorithm
    * @return cipher
    */
   public static Cipher getCipher(int mode, String algo)
   {
      if(algo == null)
         algo = DEFAULT_CIPHER_ALGORITHM;
      Cipher cipher = null;
      boolean correctMode = (mode == Cipher.ENCRYPT_MODE
                || mode == Cipher.DECRYPT_MODE);
      if(!correctMode)
         throw new IllegalArgumentException("Cipher Mode is wrong");

       try
      {
         cipher = Cipher.getInstance(algo);
         Key key = (Key)keys.get(canonicalize(algo));
         if(key == null)
            throw new IllegalStateException("Key is null for algo="+algo);
         initializeCipher(cipher,key,algo,mode);
      }
      catch (Throwable e)
      {
         log.error("getCipher failed", e);
      }
       return cipher;
   }

   /**
    * Obtain an initialized cipher given the Cipher mode,
    * algorithm and key
    * @param mode Cipher.ENCRYPT_MODE or Cipher.DECRYPT_MODE
    * @param algo
    * @param key
    * @return initialized cipher
    */
   public static Cipher getCipher(int mode, String algo, Key key)
   {
      Cipher cipher = null;
      boolean correctMode = (mode == Cipher.ENCRYPT_MODE
                || mode == Cipher.DECRYPT_MODE);
      if(!correctMode)
         throw new IllegalArgumentException("Cipher Mode is wrong");

       try
      {
         cipher = Cipher.getInstance(algo);
         initializeCipher(cipher,key,algo,mode);
      }
      catch (Throwable e)
      {
         if(log.isTraceEnabled())
            log.trace("getCipher failed:", e);
      }
       return cipher;
   }

   /**
    * Load the serialized key
    * @param algo
    * @return
    * @throws Exception
    */
   private static Key loadKey(String algo) throws Exception
   {
      ClassLoader tcl = (ClassLoader) AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            return Thread.currentThread().getContextClassLoader();
         }
      });
      String file = "org/jboss/remoting/marshall/encryption/"+algo+".key";
      InputStream is = tcl.getResourceAsStream(file);
      if(is == null)
         throw new IllegalStateException("Key file is not locatable");
      ObjectInput out = new ObjectInputStream(is);
      Key key = (Key)out.readObject();
      out.close();
      return key;
   }

   //Remove padding etc from the key algo
   private static String canonicalize(String algo)
   {
      if(algo == null)
         throw new IllegalArgumentException("Null algorithm passed");
      String result = algo;
      if(algo.indexOf("/")> 0)
      {
         result = algo.substring(0,algo.indexOf("/"));
      }
      return result;
   }

   /**
    * Initialize the Cipher
    * @param cipher Cipher
    * @param key Key
    * @param algo Algorithm
    * @param mode Cipher Mode
    * @throws Exception
    */
   private static void initializeCipher(Cipher cipher, Key key, String algo, int mode)
   throws Exception
   {
      //No Padding required
      if(algo.equals("AES") || algo.equals("DES") || algo.equals("DESede") ||
            algo.equals("RC4") || algo.equals("Blowfish"))
         cipher.init(mode, key);
      else
      if(algo.indexOf("AES") == 0 && algo.indexOf("AES/ECB") < 0 )
         cipher.init(mode, key,iv16);
      else
      if(algo.indexOf("/CBC/") > 0 || algo.indexOf("/OFB/") > 0 ||
            algo.indexOf("/PCBC/") > 0 || algo.indexOf("/CFB/") > 0)
         cipher.init(mode, key,iv8);
      else
         cipher.init(mode, key);
   }
}
