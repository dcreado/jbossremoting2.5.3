/**
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.test.remoting.transport.socket.serverlockup;

import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.invocation.NameBasedInvocation;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.logging.Logger;

import javax.management.MBeanServer;

/**
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision: 1814 $</tt>
 *
 * $Id: SimpleServerInvocationHandler.java 1814 2007-01-13 12:42:36Z ovidiu $
 */
public class SimpleServerInvocationHandler implements ServerInvocationHandler
{
   // Constants ------------------------------------------------------------------------------------

   private static final Logger log = Logger.getLogger(SimpleServerInvocationHandler.class);

   // Static ---------------------------------------------------------------------------------------

   // Attributes -----------------------------------------------------------------------------------

   // Constructors ---------------------------------------------------------------------------------

   // ServerInvocationHandler implementation -------------------------------------------------------

   public void setMBeanServer(MBeanServer server)
   {
   }

   public void setInvoker(ServerInvoker invoker)
   {
   }

   public Object invoke(InvocationRequest invocation) throws Throwable
   {
      NameBasedInvocation nbi = (NameBasedInvocation)invocation.getParameter();

      String methodName = nbi.getMethodName();

      if ("ping".equals(methodName))
      {
         return "pong." + nbi.getParameters()[0];
      }

      return null;
   }

   public void addListener(InvokerCallbackHandler callbackHandler)
   {
   }

   public void removeListener(InvokerCallbackHandler callbackHandler)
   {
   }

   // Public ---------------------------------------------------------------------------------------

   // Package protected ----------------------------------------------------------------------------

   // Protected ------------------------------------------------------------------------------------

   // Private --------------------------------------------------------------------------------------

   // Inner classes --------------------------------------------------------------------------------

}
