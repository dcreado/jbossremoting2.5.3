/*
 * Copyright 1999,2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 


package org.jboss.remoting.transport.coyote;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class handles reading bytes.
 *
 * @author Remy Maucherat
 * @author Jean-Francois Arcand
 */
public class CoyoteInputStream
      extends InputStream
{

   // ----------------------------------------------------- Instance Variables


   protected InputBuffer ib;

   // ----------------------------------------------------------- Constructors


   protected CoyoteInputStream(InputBuffer ib)
   {
      this.ib = ib;
   }

   // -------------------------------------------------------- Package Methods


   /**
    * Clear facade.
    */
   void clear()
   {
      ib = null;
   }

   // --------------------------------------------------------- Public Methods


   /**
    * Prevent cloning the facade.
    */
   protected Object clone()
         throws CloneNotSupportedException
   {
      throw new CloneNotSupportedException();
   }

   // --------------------------------------------- ServletInputStream Methods


   public int read()
         throws IOException
   {
      return ib.readByte();
   }

   public int available() throws IOException
   {
      return ib.available();
   }

   public int read(final byte[] b) throws IOException
   {
      return ib.read(b, 0, b.length);
   }


   public int read(final byte[] b, final int off, final int len)
         throws IOException
   {
      return ib.read(b, off, len);
   }


   /**
    * Close the stream
    * Since we re-cycle, we can't allow the call to super.close()
    * which would permantely disable us.
    */
   public void close() throws IOException
   {
      ib.close();
   }

}
