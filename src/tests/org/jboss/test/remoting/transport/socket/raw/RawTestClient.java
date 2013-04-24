package org.jboss.test.remoting.transport.socket.raw;

import junit.framework.TestCase;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.jboss.remoting.Version;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class RawTestClient extends TestCase
{
   protected String address = "localhost";
   protected int port = 6700;

   public boolean enableTcpNoDelay = false;
   public int timeout = 60000;

   private Socket socket = null;

   private OutputStream out;
   private InputStream in;
   private ObjectOutputStream oos;
   private ObjectInputStream objInputStream;

   public void testRawInvocation()
   {
      makeRawInvocation();
      makeRawInvocation();
   }

   public void makeRawInvocation()
   {
      // In response to JBREM-692 (Let marshallers/unmarshallers construct
      // their preferred streams.), some changes are necessary to make this
      // test work.  SerializableMarshaller and SerializableUnMarshaller now
      // implement the method getMarshallingStream(), which allows
      // ClientSocketWrapper and ServerSocketWrapper to get object streams
      // when they are created and cache them for future use, instead of
      // recreating them with each invocation.
      
         try
         {
            getSocket();
            
            // Write version.
//          out.write(1);
            oos.write(Version.getDefaultVersion());
            out.flush();
            
            // Write invocation.
//          oos = new ObjectOutputStream(out);
            oos.reset();
            oos.writeObject("This is the request");
            oos.flush();

            // Get response.
//          objInputStream = new ObjectInputStream(in);
            Object obj = objInputStream.readObject();
            System.out.println("response: " + obj);
            assertEquals(RawTestServer.RESPONSE, obj);
            System.out.println("PASSED");
         }
         catch(IOException e)
         {
            e.printStackTrace();
            fail();
         }
         catch(ClassNotFoundException e)
         {
            e.printStackTrace();
            fail();
         }

   }

   public void getSocket() throws IOException
   {
      if(socket == null)
      {
         try
         {
            socket = new Socket(address, port);
            socket.setTcpNoDelay(enableTcpNoDelay);
//            socket.setSoTimeout(timeout);

            out = new BufferedOutputStream(socket.getOutputStream());
            in = new BufferedInputStream(socket.getInputStream());
            oos = new ObjectOutputStream(out);
            objInputStream = new ObjectInputStream(in);
         }
         catch(IOException e)
         {
            e.printStackTrace();
         }
      }
      else
      {
//         oos.reset();
//         oos.writeByte(1);
//         oos.flush();
//         oos.reset();
//         objInputStream.readByte();
//         objInputStream.reset();
      }
   }

   public static void main(String[] args)
   {
      RawTestClient client = new RawTestClient();
      client.testRawInvocation();
   }
}
