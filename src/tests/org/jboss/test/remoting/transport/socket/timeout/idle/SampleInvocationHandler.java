package org.jboss.test.remoting.transport.socket.timeout.idle;

import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.apache.log4j.Logger;

import javax.management.MBeanServer;

public class SampleInvocationHandler implements ServerInvocationHandler
{
   private static Logger logger = Logger.getRootLogger();
   private boolean first = true;

   public Object invoke(InvocationRequest invocation) throws Throwable
   {
      Object parm = invocation.getParameter();
      if(parm instanceof Boolean)
      {
         synchronized(this)
         {
            if(first)
            {
               first = false;
               return Boolean.TRUE;
            }
            else
            {
               return Boolean.FALSE;
            }
         }
      }
      System.out.println(Thread.currentThread() + "******* Invoked " + parm);
      //Thread.sleep(5000);
      System.out.println(Thread.currentThread() + "******* Returning - response" + parm);
      String s = "response" + parm;
      return s;
   }

   public void setMBeanServer(MBeanServer server)
   {
   }

   public void setInvoker(ServerInvoker invoker)
   {
   }

   public void addListener(InvokerCallbackHandler callbackHandler)
   {
   }

   public void removeListener(InvokerCallbackHandler callbackHandler)
   {
   }

}
