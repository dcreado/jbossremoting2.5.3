package org.jboss.test.remoting.transport.http.ssl.timeout;

import java.util.Map;

import org.jboss.remoting.transport.http.ssl.HTTPSClientInvoker;
import org.jboss.test.remoting.timeout.SSLPerInvocationTimeoutTestRoot;


/**
 * See javadoc for PerInvocationTimeoutTestRoot.
 *   
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2207 $
 * <p>
 * Copyright Feb 6, 2007
 * </p>
 */
public class HttpsPerInvocationTimeoutTestCase extends SSLPerInvocationTimeoutTestRoot
{
   protected void addClientConfig(Map config)
   {  
      super.addClientConfig(config);
      config.put(HTTPSClientInvoker.IGNORE_HTTPS_HOST, "true");
   }
   
   protected String getTransport()
   {
      return "https";
   }
}
