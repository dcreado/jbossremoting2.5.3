package org.jboss.test.remoting.transport.bisocket.shutdown;

import java.util.HashMap;
import java.util.Map;

import org.jboss.remoting.transport.bisocket.Bisocket;
import org.jboss.test.remoting.shutdown.AbstractClient;
import org.jboss.test.remoting.shutdown.ClosingClient;

public class BisocketClosingClient extends ClosingClient
{
   public static void main(String[] args)
   {
      try
      {
         if (args.length == 0)
            throw new RuntimeException();
         
         String transport = args[0];
         
         HashMap config = new HashMap();
         System.out.println("args.length: " + args.length);
         if (args.length > 1)
            getConfig(config, args[1]);
         
         AbstractClient client = new BisocketClosingClient(transport, config);
         client.testShutdown();
      }
      catch (Throwable t)
      {
         t.printStackTrace();
      }
   }
   
   public BisocketClosingClient(String transport, Map config)
   {
      super(transport, config);
   }

   protected void addCallbackArgs(Map map)
   {
      map.put(Bisocket.IS_CALLBACK_SERVER, "true");
   }
}
