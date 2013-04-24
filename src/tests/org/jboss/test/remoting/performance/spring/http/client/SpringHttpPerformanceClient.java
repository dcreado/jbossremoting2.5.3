/***************************************
 *                                     *
 *  JBoss: The OpenSource J2EE WebOS   *
 *                                     *
 *  Distributable under LGPL license.  *
 *  See terms of license at gnu.org.   *
 *                                     *
 ***************************************/
package org.jboss.test.remoting.performance.spring.http.client;

import EDU.oswego.cs.dl.util.concurrent.Latch;
import junit.framework.Test;
import org.jboss.jrunit.decorators.ThreadLocalDecorator;
import org.jboss.logging.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.test.remoting.performance.spring.http.web.SpringHttpServer;
import org.jboss.test.remoting.performance.synchronous.PerformanceCallbackKeeper;
import org.jboss.test.remoting.performance.synchronous.PerformanceClientTest;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.remoting.rmi.RmiServiceExporter;

import java.rmi.server.UID;
import java.util.Map;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class SpringHttpPerformanceClient extends PerformanceClientTest
{

   private SpringHttpServer springHttpServerService;
   private String clientSessionId = new UID().toString();

   protected static final Logger log = Logger.getLogger(SpringHttpPerformanceClient.class);

   public static Test suite()
   {
      return new ThreadLocalDecorator(SpringHttpPerformanceClient.class, 1);
   }

   public SpringHttpServer getSpringHttpServerService()
   {
      return springHttpServerService;
   }

   public void setSpringHttpServerService(SpringHttpServer springHttpServerService)
   {
      this.springHttpServerService = springHttpServerService;
   }

   public void init()
   {

      Resource res = new ClassPathResource("SpringHttpClientService.xml", SpringHttpPerformanceClient.class);
      BeanFactory factory = new XmlBeanFactory(res);
      springHttpServerService = (SpringHttpServer)factory.getBean("springHttpServerService");


      /*
      try
      {
//         HessianProxyFactoryBean factory = new HessianProxyFactoryBean();
         BurlapProxyFactoryBean factory = new BurlapProxyFactoryBean();
         factory.setServiceInterface(SpringHttpServer.class);
         factory.setServiceUrl("http://localhost:8080/remoting/springHessianServerService");
         factory.afterPropertiesSet();
         springHessianServerService = (SpringHttpServer) factory.getObject();
      }
      catch (MalformedURLException e)
      {
         e.printStackTrace();
      }
      */

   }

   /**
    * This will be used to create callback server
    *
    * @param port
    * @return
    * @throws Exception
    */
   protected InvokerLocator initServer(int port) throws Exception
   {
      return null;
   }

   protected PerformanceCallbackKeeper addCallbackListener(String sessionId, Latch serverDoneLock)
         throws Throwable
   {
      String springServiceXml = this.getClass().getResource("SpringHttpClientService.xml").getFile();

      ApplicationContext context = null;
      for (int i = 0; i < 10; i++)
      {
         try
         {
            context = new FileSystemXmlApplicationContext(springServiceXml);
            if (context != null)
               break;
         }
         catch (Exception e)
         {
            Thread.sleep(2000);
         }
      }
      
      if (context == null)
      {
         log.error("unable to create FileSystemXmlApplicationContext from SpringHttpClientService.xml");
         throw new Exception("unable to create FileSystemXmlApplicationContext from SpringHttpClientService.xml");
      }
      
      SpringHttpCallbackServer callbackServer = (SpringHttpCallbackServer) context.getBean("springHttpCallbackServerService");
      callbackServer.setClientSessionId(clientSessionId);
      callbackServer.setServerDoneLock(serverDoneLock);
      
/*
      Instead of exporting callback servers by injection, the following xml declaration is
      carried out programmatically.  Each callback server is registered under a name
      ending in the sessionId.
    
   <bean class="org.springframework.remoting.rmi.RmiServiceExporter">
      <property name="serviceName" value="SpringHttpCallbackServerService"/>
      <property name="service" ref="springHttpCallbackServerService"/>
      <property name="servicePort" value="1300"/>
      <property name="serviceInterface" value="org.jboss.test.remoting.performance.spring.http.client.SpringHttpCallbackServer"/>
      <property name="registryPort" value="1299"/>
   </bean> 
*/
      for (int i = 0; i < 10; i++)
      {
         try
         {
            RmiServiceExporter exporter = new RmiServiceExporter();
            exporter.setServiceName("SpringHttpCallbackServerService:" + clientSessionId);
            exporter.setService(callbackServer);
            exporter.setServiceInterface(org.jboss.test.remoting.performance.spring.http.client.SpringHttpCallbackServer.class);
            exporter.setRegistryPort(1299);
            exporter.afterPropertiesSet();
            log.info("exported SpringHttpCallbackServerService:" + clientSessionId);
            break;
         }
         catch (Exception e)
         {
            Thread.sleep(2000);
         }
      }
      
      return callbackServer;

//      RMICallbackServer callbackServer = new RMICallbackServer(clientSessionId, serverDoneLock);
//      callbackServer.start();
//      return callbackServer;
   }

   protected void populateMetadata(Map metadata)
   {
      super.populateMetadata(metadata);
      metadata.put("transport", "spring_http");
      metadata.put("serialization", "java");
   }

   protected Object getBenchmarkAlias()
   {
      String config = System.getProperty("alias");
      if(config == null || config.length() == 0)
      {
         config = System.getProperty("jboss-junit-configuration");
         if(config == null || config.length() == 0)
         {
            config = "spring_http" + "_" + getNumberOfCalls() + "_" + getPayloadSize() + "_" + "java";
         }
      }
      return config;
   }


   protected Object makeInvocation(String method, Object param) throws Throwable
   {
      while (true)
      {
         try
         {
            if(method.equals(NUM_OF_CALLS))
            {
               return springHttpServerService.sendNumberOfCalls(clientSessionId, param);
            }
            else if(method.equals(TEST_INVOCATION))
            {
               return springHttpServerService.makeCall(clientSessionId, param);
            }
            else
            {
               throw new Exception("Was not able to find remote method call for " + method);
            }
         }
         catch (Exception e)
         {
            log.error("invocation error: " + e);
         }
      }
   }

   public static void main(String[] args)
   {
      SpringHttpPerformanceClient test = new SpringHttpPerformanceClient();
      try
      {
         test.setUp();
         test.testClientCalls();
         test.tearDown();
      }
      catch(Throwable throwable)
      {
         throwable.printStackTrace();
      }
   }

}
