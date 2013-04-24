package org.jboss.remoting.transporter;

import java.util.ArrayList;

/**
 * Inferface definition for any LoadBalancer implementation
 *
 * @author Jeanette Cheng</a>
 */
public interface LoadBalancer
{
   public int selectServer(ArrayList serversList);
}
