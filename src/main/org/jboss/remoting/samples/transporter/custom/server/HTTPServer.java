package org.jboss.remoting.samples.transporter.custom.server;

import org.jboss.remoting.detection.jndi.JNDIDetector;
import org.jboss.remoting.network.NetworkRegistry;
import org.jboss.remoting.samples.transporter.basic.CustomerProcessor;
import org.jboss.remoting.samples.transporter.basic.CustomerProcessorImpl;
import org.jboss.remoting.transporter.InternalTransporterServices;
import org.jboss.remoting.transporter.TransporterServer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class HTTPServer
{
   private TransporterServer server = null;

   public void start() throws Exception
   {
      initTransporterServices();

      Element xmlConfig = getXmlConfig();

      server = TransporterServer.createTransporterServer(xmlConfig, new CustomerProcessorImpl(),
                                                         CustomerProcessor.class.getName(), true);
   }

   private Element getXmlConfig() throws ParserConfigurationException, IOException, SAXException
   {

      String transport = "http";
      String host = "localhost";
      int port = 5600;

      StringBuffer buf = new StringBuffer();
      buf.append("<?xml version=\"1.0\"?>\n");
      buf.append("<config>");
      buf.append("<invoker transport=\"" + transport + "\">");
      buf.append("<attribute name=\"serverBindAddress\">" + host + "</attribute>");
      buf.append("<attribute name=\"serverBindPort\">" + port + "</attribute>");
      buf.append("</invoker>");
      buf.append("</config>");
      Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(buf.toString().getBytes()));
      return xml.getDocumentElement();

   }

   public void stop()
   {
      if (server != null)
      {
         server.stop();
      }
   }

   private void initTransporterServices() throws Exception
   {
      // create MBeanServer
      MBeanServer mbeanServer = MBeanServerFactory.createMBeanServer();

      NetworkRegistry registry = NetworkRegistry.getInstance();

      String host = InetAddress.getLocalHost().getHostAddress();
      JNDIDetector jndiDetector = new JNDIDetector();
      jndiDetector.setPort(JNDIServer.JNDI_PORT);
      jndiDetector.setHost(host);
      jndiDetector.setContextFactory("org.jnp.interfaces.NamingContextFactory");
      jndiDetector.setURLPackage("org.jboss.naming:org.jnp.interfaces");


      InternalTransporterServices transporterService = InternalTransporterServices.getInstance();

      transporterService.setup(mbeanServer,
                               jndiDetector, new ObjectName("remoting:type=Detector,transport=jndi"),
                               registry, new ObjectName("remoting:type=NetworkRegistry"),
                               true, true);

      //TODO: -TME Have to start the detector after setup() call?
      jndiDetector.start();

   }


   public static void main(String[] args)
   {
      HTTPServer server = new HTTPServer();
      try
      {
         server.start();

         Thread.currentThread().sleep(60000);

      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      finally
      {
         server.stop();
      }
   }
}
