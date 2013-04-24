/**
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.test.remoting.transport.socket.serverlockup;

import org.jboss.remoting.transport.socket.MicroSocketClientInvoker;
import org.jboss.remoting.InvokerLocator;
import org.jboss.logging.Logger;

import java.net.Socket;

/**
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision: 1972 $</tt>
 *          <p/>
 *          $Id: RogueClientInvoker.java 1972 2007-01-23 09:39:03Z ovidiu $
 */
public class RogueClientInvoker extends MicroSocketClientInvoker
{
   // Constants ------------------------------------------------------------------------------------

   private static final Logger log = Logger.getLogger(RogueClientInvoker.class);

   // Static ---------------------------------------------------------------------------------------

   // Attributes -----------------------------------------------------------------------------------

   // Constructors ---------------------------------------------------------------------------------

   public RogueClientInvoker(InvokerLocator locator)
   {
      super(locator);
   }

   // Public ---------------------------------------------------------------------------------------

   public String toString()
   {
      return "RogueClientInvoker[" + Integer.toHexString(hashCode()) + "]";
   }

   // Package protected ----------------------------------------------------------------------------

   void openConnectionButDontSendAnything() throws Exception
   {
      log.debug(this + " creating simple socket");

      Socket socket = new Socket(locator.getHost(), locator.getPort());

      log.debug(this + " created socket " + socket + ", sleeping ...");

      Thread.sleep(30000);

      log.debug(this + " done sleeping");



//      OutputStream outputStream = socketWrapper.getOutputStream();
//
//      writeVersion(outputStream, version);
//
//      versionedWrite(outputStream, marshaller, invocation, version);
//
//      InputStream inputStream = socketWrapper.getInputStream();
//
//      version = readVersion(inputStream);
//
//      response = versionedRead(inputStream, unmarshaller, version);
   }

   // Protected ------------------------------------------------------------------------------------

   // Private --------------------------------------------------------------------------------------

   // Inner classes --------------------------------------------------------------------------------
}
