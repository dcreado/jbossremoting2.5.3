package org.jboss.remoting.security;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

/**
 * This is a basic wrapper around the SSLSocketBuilder which is needed
 * because it extneds the javax.net.ServerSocketFactory class and
 * implements the SSLServerSocketFactoryServiceMBean.  It has no other function.
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class SSLSocketFactoryService extends CustomSSLSocketFactory implements SSLSocketFactoryServiceMBean
{
   public SSLSocketFactoryService()
   {
      super();
   }

   /**
    * Constructor for {@link CustomSSLSocketFactory}. The factory can be <code>null</code> - call
    * {@link #setFactory(javax.net.ssl.SSLSocketFactory)} to set it later.
    *
    * @param factory the true factory this class delegates to
    * @param builder the class that built this custom factory - contains all the configuration for this factory
    */
   public SSLSocketFactoryService(SSLSocketFactory factory, SSLSocketBuilder builder)
   {
      super(factory, builder);
   }

   /**
    * start the service, create is already called
    */
   public void start() throws Exception
   {
      if(getSSLSocketBuilder() != null)
      {
         SocketFactory socketFactory = getSSLSocketBuilder().createSSLSocketFactory();
         if(socketFactory instanceof SSLSocketFactory)
         {
            setFactory((SSLSocketFactory)socketFactory);
         }
         else
         {
            throw new Exception("Can not start SSLSocketFactoryService because socket factory produced does not support SSL.");
         }
      }
      else
      {
         throw new Exception("Can not create socket factory due to the SSLSocketBuilder not being set.");
      }
   }

   /**
    * create the service, do expensive operations etc
    */
   public void create() throws Exception
   {
      //NOOP
   }

   /**
    * stop the service
    */
   public void stop()
   {
      //NOOP
   }

   /**
    * destroy the service, tear down
    */
   public void destroy()
   {
      //NOOP
   }

}
