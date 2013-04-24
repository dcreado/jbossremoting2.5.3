/*
 * JBoss, Home of Professional Open Source
 */
package org.jboss.remoting.security;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

/**
 * SSL server socket factory whose configuration is customized.
 *
 * @author  <a href="mailto:mazz@jboss.com">John Mazzitelli</a>
 * @version $Revision: 3839 $
 */
public class CustomSSLServerSocketFactory
   extends SSLServerSocketFactory
{
   private SSLServerSocketFactory theDelegate;
   private SSLSocketBuilderMBean       theBuilder;

   /**
    * Constructor for {@link CustomSSLServerSocketFactory}. The factory can be <code>null</code> - call
    * {@link #setFactory(SSLServerSocketFactory)} to set it later.
    *
    * @param factory the true factory this class delegates to
    * @param builder the class that built this custom factory - contains all the configuration for this factory
    */
   public CustomSSLServerSocketFactory( SSLServerSocketFactory factory,
                                        SSLSocketBuilderMBean       builder )
   {
      super();
      theBuilder  = builder;
      theDelegate = factory;
   }

   public CustomSSLServerSocketFactory()
   {

   }

   /**
    * Sets the builder that creates the true socket server factory.
    * @param sslSocketBuilder
    */
   public void setSSLSocketBuilder(SSLSocketBuilderMBean sslSocketBuilder)
   {
      this.theBuilder = sslSocketBuilder;
   }

   /**
    * Returns the builder that created this factory.  You can obtain the configuration of this factory
    * by examining the returned object's configuration.
    *
    * @return the builder
    */
   public SSLSocketBuilderMBean getSSLSocketBuilder()
   {
      return theBuilder;
   }

   /**
    * Sets a new factory in this object - this is the factory that this object will use to create new sockets.
    *
    * @param  factory the new factory
    *
    * @throws IllegalArgumentException if factory is <code>null</code>
    */
   public void setFactory( SSLServerSocketFactory factory )
   {
      if ( factory == null )
      {
         throw new IllegalArgumentException( "Factory cannot be null" );
      }

      theDelegate = factory;
   }

   /**
    * @see javax.net.ServerSocketFactory#createServerSocket()
    */
   public ServerSocket createServerSocket()
   throws IOException
   {
      SSLServerSocket sock = (SSLServerSocket) theDelegate.createServerSocket();
      setSocketModes( sock );
      return sock;
   }

   /**
    * @see javax.net.ServerSocketFactory#createServerSocket(int)
    */
   public ServerSocket createServerSocket( int port )
   throws IOException
   {
      SSLServerSocket sock = (SSLServerSocket) theDelegate.createServerSocket( port );
      setSocketModes( sock );
      return sock;
   }

   /**
    * @see javax.net.ServerSocketFactory#createServerSocket(int, int)
    */
   public ServerSocket createServerSocket( int port,
                                           int backlog )
   throws IOException
   {
      SSLServerSocket sock = (SSLServerSocket) theDelegate.createServerSocket( port, backlog );
      setSocketModes( sock );
      return sock;
   }

   /**
    * @see javax.net.ServerSocketFactory#createServerSocket(int, int, java.net.InetAddress)
    */
   public ServerSocket createServerSocket( int         port,
                                           int         backlog,
                                           InetAddress ifAddress )
   throws IOException
   {
      SSLServerSocket sock = (SSLServerSocket) theDelegate.createServerSocket( port, backlog, ifAddress );
      setSocketModes( sock );
      return sock;
   }

   /**
    * @see javax.net.ssl.SSLServerSocketFactory#getDefaultCipherSuites()
    */
   public String[] getDefaultCipherSuites()
   {
      return theDelegate.getDefaultCipherSuites();
   }

   /**
    * @see javax.net.ssl.SSLServerSocketFactory#getSupportedCipherSuites()
    */
   public String[] getSupportedCipherSuites()
   {
      return theDelegate.getSupportedCipherSuites();
   }

   /**
    * @see java.lang.Object#equals(java.lang.Object)
    */
   public boolean equals( Object obj )
   {
      return theDelegate.equals( obj );
   }

   /**
    * @see java.lang.Object#hashCode()
    */
   public int hashCode()
   {
      return theDelegate.hashCode();
   }

   /**
    * @see java.lang.Object#toString()
    */
   public String toString()
   {
      return theDelegate.toString();
   }

   /**
    * Sets the socket modes according to the custom configuration.
    *
    * @param sock the socket whose modes are to be set
    */
   private void setSocketModes( SSLServerSocket sock )
   {
      if ( theBuilder != null )
      {
         
         boolean isServerSocketUseClientMode = ((Boolean)AccessController.doPrivileged( new PrivilegedAction()
         {
            public Object run()
            {
               return new Boolean(theBuilder.isServerSocketUseClientMode());
            }
         })).booleanValue();
         
         boolean isClientAuthModeWant = ((Boolean)AccessController.doPrivileged( new PrivilegedAction()
         {
            public Object run()
            {
               return new Boolean(theBuilder.isClientAuthModeWant());
            }
         })).booleanValue();
         
         boolean isClientAuthModeNeed = ((Boolean)AccessController.doPrivileged( new PrivilegedAction()
         {
            public Object run()
            {
               return new Boolean(theBuilder.isClientAuthModeNeed());
            }
         })).booleanValue();

         sock.setUseClientMode( isServerSocketUseClientMode );
         
         if ( isClientAuthModeWant )
         {
            sock.setNeedClientAuth( false );
            sock.setWantClientAuth( true );
         }
         else if ( isClientAuthModeNeed )
         {
            sock.setWantClientAuth( false );
            sock.setNeedClientAuth( true );
         }
         else
         {
            sock.setWantClientAuth( false );
            sock.setNeedClientAuth( false );
         }
      }

      return;
   }
}