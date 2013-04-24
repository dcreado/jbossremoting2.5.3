package org.jboss.test.remoting.transport.http.timeout;

import java.util.HashMap;

import org.jboss.remoting.ServerInvoker;
import org.jboss.test.remoting.timeout.PerInvocationTimeoutTestRoot;


/**
 * See javadoc for PerInvocationTimeoutTestRoot.
 *   
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2201 $
 * <p>
 * Copyright Feb 6, 2007
 * </p>
 */
public class HttpPerInvocationTimeoutTestCase extends PerInvocationTimeoutTestRoot
{    
   /**
    * There seems to be some strange behavior in the BasicThreadPool timeout 
    * mechanism, which could potentially cause a thread to prematurely time out.
    */
   public void testBasicThreadPool() throws Throwable
   {
      log.info("entering " + getName());
      
      try
      {
         for (int i = 0; i < 1000; i++)
         {
            HashMap metadata = new HashMap();
            metadata.put(ServerInvoker.TIMEOUT, "1000");
            assertEquals(NO_WAIT, client.invoke(NO_WAIT, metadata));
         }
      }
      catch (Exception e)
      {
         log.error(e);
         fail();
      }
      
      log.info(getName() + " PASSES");
   }
   
   
   protected String getTransport()
   {
      return "http";
   }
}
