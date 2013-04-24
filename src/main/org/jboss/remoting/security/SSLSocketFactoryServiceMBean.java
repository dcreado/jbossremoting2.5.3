package org.jboss.remoting.security;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public interface SSLSocketFactoryServiceMBean extends SocketFactoryMBean
{
   /**
    * create the service, do expensive operations etc
    */
   void create() throws Exception;

   /**
    * start the service, create is already called
    */
   void start() throws Exception;

   /**
    * stop the service
    */
   void stop();

   /**
    * destroy the service, tear down
    */
   void destroy();

   void setSSLSocketBuilder(SSLSocketBuilderMBean sslSocketBuilder);

}
