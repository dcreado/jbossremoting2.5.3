package org.jboss.test.remoting.transport.socket.timeout.idle;

import org.apache.log4j.Logger;
import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transport.Connector;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class IdleTimeoutTestServer extends ServerTestCase
{
   private Connector connector;

   private Logger logger = Logger.getRootLogger();

   protected String getTransport()
   {
      return "socket";
   }
   
   public static void main(String[] args) throws Throwable
   {
      IdleTimeoutTestServer rt = new IdleTimeoutTestServer();
      rt.startServer();
   }

   public void setUp() throws Exception
   {
      startServer();
   }

   public void tearDown() throws Exception
   {
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   public void startServer() throws Exception
   {
//      String locatorURI = "socket://localhost:54000/?maxPoolSize=2&timeout=60000&backlog=0";
      String locatorURI = getTransport() + "://localhost:54000/?maxPoolSize=2&backlog=0&timeout=60000&idleTimeout=5";
      InvokerLocator locator = new InvokerLocator(locatorURI);

      connector = new Connector();

      connector.setInvokerLocator(locator.getLocatorURI());
      connector.create();

      SampleInvocationHandler invocationHandler = new SampleInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
   }

}

