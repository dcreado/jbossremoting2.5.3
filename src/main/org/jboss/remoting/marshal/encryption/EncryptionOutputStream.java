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

import java.io.IOException;
import java.io.OutputStream; 

//$Id: EncryptionOutputStream.java 1368 2006-08-16 19:18:30Z asaldhana $

/**
 *  OutputStream that is piped into a CipherOutputStream such that
 *  CipherOutputStream.close will not close the underlying stream
 *  @author <a href="mailto:Anil.Saldhana@jboss.org">Anil Saldhana</a>
 *  @since  Aug 16, 2006 
 *  @version $Revision: 1368 $
 */
public class EncryptionOutputStream extends OutputStream
{ 
   private OutputStream delegate; 

   public EncryptionOutputStream(OutputStream os)
   {
      this.delegate = os; 
   }
   
   /**
    * @see OutputStream#write(int)
    */
   public void write(int a) throws IOException
   { 
      delegate.write(a);
   }
 
   /**
    * @see OutputStream#close()
    */
   public void close() throws IOException
   {
     
     //Flush only. Do not close the stream
     delegate.flush(); 
   }
 
   /**
    * @see OutputStream#flush()
    */
   public void flush() throws IOException
   {
     delegate.flush();
   }
 
   /**
    * @see OutputStream#write(byte[], int, int)
    */
   public void write(byte[] b, int off, int len) throws IOException
   {
      delegate.write(b, off, len);
   }
 
   /**
    * @see OutputStream#write(byte[])
    */
   public void write(byte[] b) throws IOException
   {
      delegate.write(b);
   } 
   
   /**
    * @see Object#equals(Object)
    */
   public boolean equals(Object obj)
   { 
      return delegate.equals(obj);
   }
 
   /**
    * @see Object#hashCode()
    */
   public int hashCode()
   { 
      return delegate.hashCode();
   }  
}
