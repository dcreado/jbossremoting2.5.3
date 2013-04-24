package org.jboss.test.remoting.callback.push.unidirectional.rmi;

import org.jboss.test.remoting.callback.push.unidirectional.CallbackPollTestClient;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class RMICallbackPollTestClient extends CallbackPollTestClient
{
   public String getTransport()
   {
      return "rmi";
   }
}
