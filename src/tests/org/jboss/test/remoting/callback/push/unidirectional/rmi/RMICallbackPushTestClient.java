package org.jboss.test.remoting.callback.push.unidirectional.rmi;

import org.jboss.test.remoting.callback.push.unidirectional.CallbackPushTestClient;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class RMICallbackPushTestClient extends CallbackPushTestClient
{
   public String getTransport()
   {
      return "rmi";
   }
}
