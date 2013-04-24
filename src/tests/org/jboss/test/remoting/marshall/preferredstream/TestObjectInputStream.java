package org.jboss.test.remoting.marshall.preferredstream;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

public class TestObjectInputStream extends ObjectInputStream
{
   public TestObjectInputStream(InputStream in) throws IOException
   {
      super(in);
   }

   public TestObjectInputStream() throws IOException, SecurityException
   {
      super();
   }
}
