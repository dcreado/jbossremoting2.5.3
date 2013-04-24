/***************************************
 *                                     *
 *  JBoss: The OpenSource J2EE WebOS   *
 *                                     *
 *  Distributable under LGPL license.  *
 *  See terms of license at gnu.org.   *
 *                                     *
 ***************************************/
package org.jboss.test.remoting.performance.spring.rmi;

import EDU.oswego.cs.dl.util.concurrent.Latch;
import junit.framework.Test;
import org.jboss.jrunit.decorators.ThreadLocalDecorator;
import org.jboss.logging.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.test.remoting.performance.synchronous.MultiThreadedPerformanceClientTest;
import org.jboss.test.remoting.performance.synchronous.PerformanceCallbackKeeper;
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
public class MultiThreadedSpringRMIPerformanceClient extends MultiThreadedPerformanceClientTest
{
   private SpringRMIServer springRMIServerService;
   private String clientSessionId = new UID().toString();

   protected static final Logger log = Logger.getLogger(MultiThreadedSpringRMIPerformanceClient.class);

   public static Test suite()
   {
      return new ThreadLocalDecorator(MultiThreadedSpringRMIPerformanceClient.class, 1);
   }

   public SpringRMIServer getSpringRMIServerService()
   {
      return springRMIServerService;
   }

   public void setSpringRMIServerService(SpringRMIServer springRMIServerService)
   {
      this.springRMIServerService = springRMIServerService;
   }

   public void init()
   {

      Resource res = new ClassPathResource("SpringRMIClientService.xml", SpringRMIPerformanceClient.class);
      BeanFactory factory = new XmlBeanFactory(res);
      springRMIServerService = (SpringRMIServer)factory.getBean("springRMIServerService");

      /*
      //super.init();

      String name = "//localhost/RMIServer";
//         RMIServer svr = (RMIServer) Naming.lookup(name);

      try
      {
         //Registry regsitry = LocateRegistry.getRegistry("localhost", rmiPort);
         Registry regsitry = LocateRegistry.getRegistry(rmiPort);
         Remote remoteObj = regsitry.lookup(name);
         rmiServer = (RMIServerRemote) remoteObj;
      }
      catch(Exception e)
      {
         log.error("Error initializating rmi client.", e);
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
      String springServiceXml = this.getClass().getResource("SpringRMIClientService.xml").getFile();

//      ApplicationContext context = new FileSystemXmlApplicationContext(springServiceXml);
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
      
      SpringRMICallbackServer callbackServer = (SpringRMICallbackServer) context.getBean("springRMICallbackServerService");
      callbackServer.setClientSessionId(clientSessionId);
      callbackServer.setServerDoneLock(serverDoneLock);
     
/*
      Instead of exporting callback servers by injection, the following xml declaration is
      carried out programmatically.  Each callback server is registered under a name
      ending in the sessionId.
    
   <bean class="org.springframework.remoting.rmi.RmiServiceExporter">
      <property name="serviceName" value="SpringRMICallbackServerService"/>
      <property name="service" ref="springRMICallbackServerService"/>
      <!--<property name="servicePort" value="1300"/>-->
      <property name="serviceInterface" value="org.jboss.test.remoting.performance.spring.rmi.SpringRMICallbackServer"/>
      <property name="registryPort" value="1299"/>
   </bean> 
*/
      for (int i = 0; i < 10; i++)
      {
         try
         {
            RmiServiceExporter exporter = new RmiServiceExporter();
            exporter.setServiceName("SpringRMICallbackServerService:" + clientSessionId);
            exporter.setService(callbackServer);
            exporter.setServiceInterface(org.jboss.test.remoting.performance.spring.rmi.SpringRMICallbackServer.class);
            exporter.setRegistryPort(1299);
            exporter.afterPropertiesSet();
            log.info("exported SpringRMICallbackServerService:" + clientSessionId);
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
      metadata.put("transport", "spring_rmi");
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
            config = "spring_rmi" + "_" + getNumberOfCalls() + "_" + getPayloadSize() + "_" + "java";
         }
      }
      return config;
   }


   protected Object makeInvocation(String method, Object param) throws Throwable
   {
      if(method.equals(NUM_OF_CALLS))
      {
         return springRMIServerService.sendNumberOfCalls(clientSessionId, param);
      }
      else if(method.equals(TEST_INVOCATION))
      {
         return springRMIServerService.makeCall(clientSessionId, param);
      }
      else
      {
         throw new Exception("Was not able to find remote method call for " + method);
      }
   }

   public static void main(String[] args)
   {
      MultiThreadedPerformanceClientTest test = new MultiThreadedSpringRMIPerformanceClient();
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