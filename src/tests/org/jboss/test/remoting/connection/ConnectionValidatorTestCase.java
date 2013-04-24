package org.jboss.test.remoting.connection;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.jboss.remoting.Client;
import org.jboss.remoting.ConnectionFailedException;
import org.jboss.remoting.ConnectionValidator;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.MicroRemoteClientInvoker;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.transport.ClientInvoker;

public class ConnectionValidatorTestCase extends TestCase
{

   public void testShouldDisallowDirectRun()
   {
      ConnectionValidator cv = new ConnectionValidator(new Client() {
         public Map getConfiguration()
         {
            return new HashMap();
         }

         public ClientInvoker getInvoker()
         {
            try
            {
               return new MicroRemoteClientInvoker(
                     new InvokerLocator("http://dummy:65535/dummy/")) {

                  public String getSessionId()
                  {
                     return "dummyId";
                  }

                  protected String getDefaultDataType()
                  {
                     throw new UnsupportedOperationException();
                  }

                  protected void handleConnect() throws ConnectionFailedException
                  {
                     throw new UnsupportedOperationException();
                  }

                  protected void handleDisconnect()
                  {
                     throw new UnsupportedOperationException();
                  }

                  protected Object transport(String sessionId, Object invocation, Map metadata, Marshaller marshaller,
                        UnMarshaller unmarshaller) throws IOException, ConnectionFailedException, ClassNotFoundException
                  {
                     throw new UnsupportedOperationException();
                  }
               };
            }
            catch (MalformedURLException e)
            {
               throw new RuntimeException(e);
            }
         }
      });

      try
      {
         cv.run();
         fail("Should throw IllegalStateException");
      } catch (IllegalStateException e) {
         // Expected
      }
   }
}
