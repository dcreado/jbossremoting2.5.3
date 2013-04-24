package org.jboss.test.remoting.callback.pull;

import org.jboss.logging.Logger;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.callback.ServerInvokerCallbackHandler;

import javax.management.MBeanServer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class CallbackInvocationHandler implements ServerInvocationHandler
{
   private transient List pullListeners = new ArrayList();

   private int numberOfCallbacks = 5;

   private static final Logger log = Logger.getLogger(CallbackInvocationHandler.class);

   /**
    * called to handle a specific invocation
    *
    * @param invocation
    * @return
    * @throws Throwable
    */
   public Object invoke(InvocationRequest invocation) throws Throwable
   {
      String id = invocation.getSessionId();
      if(pullListeners != null)
      {
         for(int x = 0; x < pullListeners.size(); x++)
         {
            ServerInvokerCallbackHandler callbackHandler = (ServerInvokerCallbackHandler)pullListeners.get(x);
            if(callbackHandler.getClientSessionId().equals(id))
            {
               callbackHandler.handleCallback(new Callback(id));
            }
         }
      }
         return "Starting callback";
   }

   public void addListener(InvokerCallbackHandler callbackHandler)
   {
         pullListeners.add(callbackHandler);
   }

   public void removeListener(InvokerCallbackHandler callbackHandler)
   {
      pullListeners.remove(callbackHandler);
   }

   /**
    * set the mbean server that the handler can reference
    *
    * @param server
    */
   public void setMBeanServer(MBeanServer server)
   {
      // NO OP as do not need reference to MBeanServer for this handler
   }

   /**
    * set the invoker that owns this handler
    *
    * @param invoker
    */
   public void setInvoker(ServerInvoker invoker)
   {
      // NO OP as do not need reference back to the server invoker
   }

}
