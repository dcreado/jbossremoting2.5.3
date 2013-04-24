/***************************************
 *                                     *
 *  JBoss: The OpenSource J2EE WebOS   *
 *                                     *
 *  Distributable under LGPL license.  *
 *  See terms of license at gnu.org.   *
 *                                     *
 ***************************************/
package org.jboss.test.remoting.performance.spring.hessian.web;

import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.test.remoting.performance.spring.hessian.client.SpringHessianCallbackServer;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class SpringHessianHandler implements InvokerCallbackHandler
{
   private SpringHessianCallbackServer springHessianCallbackServer;

   public void start()
   {
      Resource res = new ClassPathResource("SpringHessianCallbackServerService.xml", SpringHessianHandler.class);
      BeanFactory factory = new XmlBeanFactory(res);
      springHessianCallbackServer = (SpringHessianCallbackServer)factory.getBean("springHessianCallbackServerService");

   }

   public SpringHessianCallbackServer getSpringHessianCallbackServer()
   {
      return springHessianCallbackServer;
   }

   public void setSpringHessianCallbackServer(SpringHessianCallbackServer springHessianCallbackServer)
   {
      this.springHessianCallbackServer = springHessianCallbackServer;
   }

   public void handleCallback(Callback callback) throws HandleCallbackException
   {
      System.out.println("Need to make call on SpringRMICallbackServer with results. " + callback);

      try
      {
         springHessianCallbackServer.finishedProcessing(callback);
      }
      catch(Exception e)
      {
         e.printStackTrace();
         throw new HandleCallbackException(e.getMessage());
      }
   }
}
