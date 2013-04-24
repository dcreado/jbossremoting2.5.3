/***************************************
 *                                     *
 *  JBoss: The OpenSource J2EE WebOS   *
 *                                     *
 *  Distributable under LGPL license.  *
 *  See terms of license at gnu.org.   *
 *                                     *
 ***************************************/
package org.jboss.test.remoting.performance.spring.http.web;

import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.test.remoting.performance.spring.http.client.SpringHttpCallbackServer;
import org.jboss.test.remoting.performance.spring.rmi.SpringRMICallbackServer;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class SpringHttpHandler implements InvokerCallbackHandler
{
   private SpringHttpCallbackServer springHttpCallbackServer;
   private String sessionId;

   public SpringHttpHandler(String sessionId)
   {
      this.sessionId = sessionId;
   }
   
   public void start()
   {
//      Resource res = new ClassPathResource("SpringHttpCallbackServerService.xml", SpringHttpHandler.class);
//      BeanFactory factory = new XmlBeanFactory(res);
//      springHttpCallbackServer = (SpringHttpCallbackServer)factory.getBean("springHttpCallbackServerService");
    
/*
      Instead of creating callback server proxies by injection, the following xml declaration is
      replaced by programmatic creation.  Each callback server is registered under a name
      ending in the sessionId.
      
   <bean id="springHttpCallbackServerService" class="org.springframework.remoting.rmi.RmiProxyFactoryBean">
      <property name="serviceUrl" value="rmi://localhost:1299/SpringHttpCallbackServerService"/>
      <property name="serviceInterface" value="org.jboss.test.remoting.performance.spring.http.client.SpringHttpCallbackServer"/>
   </bean>
 */
      
      RmiProxyFactoryBean factory = new RmiProxyFactoryBean();
      factory.setServiceUrl("rmi://localhost:1299/SpringHttpCallbackServerService:" + sessionId);
      factory.setServiceInterface(org.jboss.test.remoting.performance.spring.http.client.SpringHttpCallbackServer.class);
      try
      {
         factory.afterPropertiesSet();
      }
      catch (Exception e)
      {
         System.out.println("unable to create callback proxy");
         System.out.println(e);
      }
      springHttpCallbackServer = (SpringHttpCallbackServer)factory.getObject(); 
   }

   public SpringHttpCallbackServer getSpringHttpCallbackServer()
   {
      return springHttpCallbackServer;
   }

   public void setSpringHttpCallbackServer(SpringHttpCallbackServer springHttpCallbackServer)
   {
      this.springHttpCallbackServer = springHttpCallbackServer;
   }

   public void handleCallback(Callback callback) throws HandleCallbackException
   {
      System.out.println("Need to make call on SpringRMICallbackServer with results. " + callback);

      try
      {
         springHttpCallbackServer.finishedProcessing(callback);
      }
      catch(Exception e)
      {
         e.printStackTrace();
         throw new HandleCallbackException(e.getMessage());
      }
   }
}
