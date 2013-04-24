package org.jboss.test.remoting.callback.push.unidirectional.http;

import org.jboss.test.remoting.callback.push.unidirectional.CallbackPollTestClient;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class HTTPCallbackPollTestClient extends CallbackPollTestClient
{
   public String getTransport()
   {
      return "http";
   }
}
