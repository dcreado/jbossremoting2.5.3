/***************************************
 *                                     *
 *  JBoss: The OpenSource J2EE WebOS   *
 *                                     *
 *  Distributable under LGPL license.  *
 *  See terms of license at gnu.org.   *
 *                                     *
 ***************************************/
package org.jboss.test.remoting.performance.spring.hessian.client;

import EDU.oswego.cs.dl.util.concurrent.Latch;
import junit.framework.Test;
import org.jboss.jrunit.decorators.ThreadLocalDecorator;
import org.jboss.logging.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.test.remoting.performance.spring.hessian.web.SpringHessianServer;
import org.jboss.test.remoting.performance.synchronous.PerformanceCallbackKeeper;
import org.jboss.test.remoting.performance.synchronous.PerformanceClientTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.remoting.caucho.HessianProxyFactoryBean;

import java.net.MalformedURLException;
import java.rmi.server.UID;
import java.util.Map;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class SpringHessianPerformanceClient extends PerformanceClientTest
{

   private SpringHessianServer springHessianServerService;
   private String clientSessionId = new UID().toString();

   protected static final Logger log = Logger.getLogger(SpringHessianPerformanceClient.class);

   public static Test suite()
   {
      return new ThreadLocalDecorator(SpringHessianPerformanceClient.class, 1);
   }

   public SpringHessianServer getSpringHessianServerService()
   {
      return springHessianServerService;
   }

   public void setSpringHessianServerService(SpringHessianServer springHessianServerService)
   {
      this.springHessianServerService = springHessianServerService;
   }

   public void init()
   {

//      Resource res = new ClassPathResource("SpringRMIClientService.xml", SpringHessianPerformanceClient.class);
//      BeanFactory factory = new XmlBeanFactory(res);
//      springHessianServerService = (SpringHessianServer)factory.getBean("springRMIServerService");


      try
      {
         HessianProxyFactoryBean factory = new HessianProxyFactoryBean();
         factory.setServiceInterface(SpringHessianServer.class);
         factory.setServiceUrl("http://localhost:8080/remoting/springHessianServerService");
         factory.afterPropertiesSet();
         springHessianServerService = (SpringHessianServer) factory.getObject();
         if (false) throw new MalformedURLException("xxx");
      }
      catch (MalformedURLException e)
      {
         e.printStackTrace();
      }

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
      String springServiceXml = this.getClass().getResource("SpringHessianClientService.xml").getFile();

      ApplicationContext context = new FileSystemXmlApplicationContext(springServiceXml);
      SpringHessianCallbackServer callbackServer = (SpringHessianCallbackServer) context.getBean("springHessianCallbackServerService");
      callbackServer.setClientSessionId(clientSessionId);
      callbackServer.setServerDoneLock(serverDoneLock);
      return callbackServer;

//      RMICallbackServer callbackServer = new RMICallbackServer(clientSessionId, serverDoneLock);
//      callbackServer.start();
//      return callbackServer;
   }

   protected void populateMetadata(Map metadata)
   {
      super.populateMetadata(metadata);
      metadata.put("transport", "spring_hessian");
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
            config = "spring_hessian" + "_" + getNumberOfCalls() + "_" + getPayloadSize() + "_" + "java";
         }
      }
      return config;
   }


   protected Object makeInvocation(String method, Object param) throws Throwable
   {
      if(method.equals(NUM_OF_CALLS))
      {
         return springHessianServerService.sendNumberOfCalls(clientSessionId, param);
      }
      else if(method.equals(TEST_INVOCATION))
      {
         return springHessianServerService.makeCall(clientSessionId, param);
      }
      else
      {
         throw new Exception("Was not able to find remote method call for " + method);
      }
   }

   public static void main(String[] args)
   {
      SpringHessianPerformanceClient test = new SpringHessianPerformanceClient();
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
