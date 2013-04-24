package org.jboss.test.remoting.marshall.preferredstream;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class TestObjectOutputStream extends ObjectOutputStream
{
   public TestObjectOutputStream(OutputStream out) throws IOException
   {
      super(out);
   }

   public TestObjectOutputStream() throws IOException, SecurityException
   {
      super();
   }
}
