package org.jboss.remoting.transporter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;

public class DefaultLoadBalancer implements LoadBalancer, Serializable
{
   private static final long serialVersionUID = -7219455363024542925L;

   public int selectServer(ArrayList servers)
   {
      int index = 0;

      if (servers != null)
      {
         int size = servers.size();
         if (size > 1)
         {
            index = new Random().nextInt(size);
         }
      }

      return index;
   }
}
