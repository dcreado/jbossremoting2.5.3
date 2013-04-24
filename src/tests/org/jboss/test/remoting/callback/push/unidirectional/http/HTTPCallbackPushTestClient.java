package org.jboss.test.remoting.callback.push.unidirectional.http;

import org.jboss.test.remoting.callback.push.unidirectional.CallbackPushTestClient;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class HTTPCallbackPushTestClient extends CallbackPushTestClient
{
   public String getTransport()
   {
      return "http";
   }
}
