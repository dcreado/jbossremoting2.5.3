/***************************************
 *                                     *
 *  JBoss: The OpenSource J2EE WebOS   *
 *                                     *
 *  Distributable under LGPL license.  *
 *  See terms of license at gnu.org.   *
 *                                     *
 ***************************************/
package org.jboss.test.remoting.performance.spring.rmi;

import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class SpringRMIHandler implements InvokerCallbackHandler
{
   private SpringRMICallbackServer springRMICallbackServer;
   private String sessionId;
   
   public SpringRMIHandler(String sessionId)
   {
      this.sessionId = sessionId;
   }

   public void start()
   {
//    Resource res = new ClassPathResource("SpringRMICallbackServerService.xml", SpringRMIHandler.class);
//    BeanFactory factory = new XmlBeanFactory(res);
//    springRMICallbackServer = (SpringRMICallbackServer)factory.getBean("springRMICallbackServerService:" );

    
/*
      Instead of creating callback server proxies by injection, the following xml declaration is
      replaced by programmatic creation.  Each callback server is registered under a name
      ending in the sessionId.
      
      <bean id="springRMICallbackServerService" class="org.springframework.remoting.rmi.RmiProxyFactoryBean">
         <property name="serviceUrl" value="rmi://localhost:1299/SpringRMICallbackServerService"/>
         <property name="serviceInterface" value="org.jboss.test.remoting.performance.spring.rmi.SpringRMICallbackServer"/>
      </bean>
 */
      
      RmiProxyFactoryBean factory = new RmiProxyFactoryBean();
      factory.setServiceUrl("rmi://localhost:1299/SpringRMICallbackServerService:" + sessionId);
      factory.setServiceInterface(org.jboss.test.remoting.performance.spring.rmi.SpringRMICallbackServer.class);
      try
      {
         factory.afterPropertiesSet();
      }
      catch (Exception e)
      {
         System.out.println("unable to create callback proxy");
         System.out.println(e);
      }
      springRMICallbackServer = (SpringRMICallbackServer)factory.getObject();      
   }

   public SpringRMICallbackServer getSpringRMICallbackServer()
   {
      return springRMICallbackServer;
   }

   public void setSpringRMICallbackServer(SpringRMICallbackServer springRMICallbackServer)
   {
      this.springRMICallbackServer = springRMICallbackServer;
   }

   public void handleCallback(Callback callback) throws HandleCallbackException
   {
      System.out.println("Need to make call on SpringRMICallbackServer with results. " + callback);

      try
      {
         springRMICallbackServer.finishedProcessing(callback);
      }
      catch(Exception e)
      {
         e.printStackTrace();
         throw new HandleCallbackException(e.getMessage());
      }
   }
}