package org.jboss.test.remoting.stream;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transport.Connector;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class StreamingConnectorTestClient extends TestCase
{
   // Default locator values
   private static String transport = "socket";
   private static String host = "localhost";
   private static int port = 5400;

   private String locatorURI;
   private Client remotingClient = null;
   private File testFile = null;
   private FileInputStream fileInput = null;

   private boolean error = false;

   private Connector connector = null;
   private String streamConnectorLocatorUri;

   public void testStream() throws Throwable
   {
      for(int x = 0; x < 5; x++)
      {
//         new Thread(new Runnable() {
//            public void run()
//            {
//               try
//               {
                  sendStream();
//               }
//               catch (Throwable throwable)
//               {
//                  throwable.printStackTrace();
//                  error = true;
//               }
//            }
//         }).start();
      }

//      Thread.sleep(5000);
//      assertFalse(error);
   }

   public void sendStream() throws Throwable
   {
      URL fileURL = this.getClass().getResource("test.txt");
      if(fileURL == null)
      {
         throw new Exception("Can not find file test.txt");
      }
      testFile = new File(fileURL.getFile());
      fileInput = new FileInputStream(testFile);

      String param = "foobar";
      long fileLength = testFile.length();
      System.out.println("File size = " + fileLength);
      Object ret = remotingClient.invoke(fileInput, param, connector);

      Map responseMap = (Map)ret;
      String subSys = (String)responseMap.get("subsystem");
      String clientId = (String)responseMap.get("clientid");
      String paramVal = (String)responseMap.get("paramval");

      assertEquals("test_stream".toUpperCase(), subSys);
      assertEquals(remotingClient.getSessionId(), clientId);
      assertEquals("foobar", paramVal);

      Object response = remotingClient.invoke("get_size");
      int returnedFileLength = ((Integer) response).intValue();
      System.out.println("Invocation response: " + response);

      if(fileLength == returnedFileLength)
      {
         System.out.println("PASS");
      }
      else
      {
         System.out.println("FAILED - returned file length was " + returnedFileLength);
      }
      assertEquals(fileLength, returnedFileLength);
   }

   public void setUp() throws Exception
   {
      String bindAddr = System.getProperty("jrunit.bind_addr", host);
      locatorURI = transport + "://" + bindAddr + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      System.out.println("Calling remoting server with locator uri of: " + locatorURI);

      remotingClient = new Client(locator, "test_stream");
      remotingClient.connect();
      setupServer();
   }

   private void setupServer() throws Exception
   {
      String bindAddr = System.getProperty("jrunit.bind_addr", host);
      streamConnectorLocatorUri = transport + "://" + bindAddr + ":" + 5410;
      connector = new Connector(streamConnectorLocatorUri);
      connector.create();
      connector.start();
   }

   public void tearDown() throws Exception
   {
      if(remotingClient != null)
      {
         remotingClient.disconnect();
      }
      if(fileInput != null)
      {
         fileInput.close();
      }
   }

   /**
    * Can pass transport and port to be used as parameters.
    * Valid transports are 'rmi' and 'socket'.
    *
    * @param args
    */
   public static void main(String[] args)
   {
      if(args != null && args.length == 2)
      {
         StreamingConnectorTestClient.transport = args[0];
         StreamingConnectorTestClient.port = Integer.parseInt(args[1]);
      }
      String locatorURI = StreamingConnectorTestClient.transport + "://" + StreamingConnectorTestClient.host + ":" + StreamingConnectorTestClient.port;
      StreamingConnectorTestClient client = new StreamingConnectorTestClient();
      try
      {
         client.setUp();
         client.testStream();
         client.tearDown();
      }
      catch(Throwable e)
      {
         e.printStackTrace();
      }
   }


}
