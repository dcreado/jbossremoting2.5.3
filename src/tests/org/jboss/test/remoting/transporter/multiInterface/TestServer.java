package org.jboss.test.remoting.transporter.multiInterface;

import java.io.IOException;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public interface TestServer
{
   public String processTestMessage(String msg);

   public void throwException() throws IOException;
}
