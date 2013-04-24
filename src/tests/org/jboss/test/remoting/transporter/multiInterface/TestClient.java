package org.jboss.test.remoting.transporter.multiInterface;

import junit.framework.TestCase;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transporter.TransporterClient;

import java.io.IOException;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class TestClient extends TestCase
{
   public void testClientCall() throws Exception
   {
      String locatorURI = "socket://localhost:5400";
      InvokerLocator locator = new InvokerLocator(locatorURI);
      TestServer test = (TestServer) TransporterClient.createTransporterClient(locator, TestServer.class);
      Object response = test.processTestMessage("Hello");
      System.out.println("response is: " + response);
      assertEquals("response should be 'Hello - has been processed'", "Hello - has been processed", response);

      // now make call that should throw exception
      try
      {
         test.throwException();
         assertTrue("Should have received IOException thrown by server.", false);
      }
      catch(IOException ioex)
      {
         assertTrue("Should have received IOException thrown by server.", true);
      }

      TransporterClient.destroyTransporterClient(test);


      TestServer2 test2 = (TestServer2) TransporterClient.createTransporterClient(locator, TestServer2.class);
      Object response2 = test2.doIt("foobar");
      System.out.println("response2 is: " + response2);
      assertEquals("response should be 'foobar'", "foobar", response2);

      TransporterClient.destroyTransporterClient(test2);

   }

}
