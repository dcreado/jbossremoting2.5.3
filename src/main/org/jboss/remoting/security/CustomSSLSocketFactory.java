/*
 * JBoss, Home of Professional Open Source
 */
package org.jboss.remoting.security;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * SSL socket factory whose configuration is customized.
 *
 * @author  <a href="mailto:mazz@jboss.com">John Mazzitelli</a>
 * @version $Revision: 1099 $
 */
public class CustomSSLSocketFactory
   extends SSLSocketFactory
{
   private SSLSocketFactory theDelegate;
   private SSLSocketBuilderMBean theBuilder;

   /**
    * Constructor for {@link CustomSSLSocketFactory}. The factory can be <code>null</code> - call
    * {@link #setFactory(SSLSocketFactory)} to set it later.
    *
    * @param factory the true factory this class delegates to
    * @param builder the class that built this custom factory - contains all the configuration for this factory
    */
   public CustomSSLSocketFactory( SSLSocketFactory factory,
                                  SSLSocketBuilder builder )
   {
      super();
      theBuilder  = builder;
      theDelegate = factory;
   }

   public CustomSSLSocketFactory()
   {

   }

   /**
    * Sets the builder that created the factory.  
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
   public void setFactory( SSLSocketFactory factory )
   {
      if ( factory == null )
      {
         throw new IllegalArgumentException( "Factory cannot be null" );
      }

      theDelegate = factory;
   }

   /**
    * @see javax.net.ssl.SSLSocketFactory#createSocket(java.net.Socket, java.lang.String, int, boolean)
    */
   public Socket createSocket( Socket  s,
                               String  host,
                               int     port,
                               boolean autoClose )
   throws IOException
   {
      SSLSocket sock = (SSLSocket) theDelegate.createSocket( s, host, port, autoClose );
      setSocketModes( sock );
      return sock;
   }

   /**
    * @see javax.net.SocketFactory#createSocket()
    */
   public Socket createSocket()
   throws IOException
   {
      SSLSocket sock = (SSLSocket) theDelegate.createSocket();
      setSocketModes( sock );
      return sock;
   }

   /**
    * @see javax.net.SocketFactory#createSocket(java.net.InetAddress, int, java.net.InetAddress, int)
    */
   public Socket createSocket( InetAddress address,
                               int         port,
                               InetAddress localAddress,
                               int         localPort )
   throws IOException
   {
      SSLSocket sock = (SSLSocket) theDelegate.createSocket( address, port, localAddress, localPort );
      setSocketModes( sock );
      return sock;
   }

   /**
    * @see javax.net.SocketFactory#createSocket(java.net.InetAddress, int)
    */
   public Socket createSocket( InetAddress host,
                               int         port )
   throws IOException
   {
      SSLSocket sock = (SSLSocket) theDelegate.createSocket( host, port );
      setSocketModes( sock );
      return sock;
   }

   /**
    * @see javax.net.SocketFactory#createSocket(java.lang.String, int, java.net.InetAddress, int)
    */
   public Socket createSocket( String      host,
                               int         port,
                               InetAddress localHost,
                               int         localPort )
   throws IOException,
          UnknownHostException
   {
      SSLSocket sock = (SSLSocket) theDelegate.createSocket( host, port, localHost, localPort );
      setSocketModes( sock );
      return sock;
   }

   /**
    * @see javax.net.SocketFactory#createSocket(java.lang.String, int)
    */
   public Socket createSocket( String host,
                               int    port )
   throws IOException,
          UnknownHostException
   {
      SSLSocket sock = (SSLSocket) theDelegate.createSocket( host, port );
      setSocketModes( sock );
      return sock;
   }

   /**
    * @see javax.net.ssl.SSLSocketFactory#getDefaultCipherSuites()
    */
   public String[] getDefaultCipherSuites()
   {
      return theDelegate.getDefaultCipherSuites();
   }

   /**
    * @see javax.net.ssl.SSLSocketFactory#getSupportedCipherSuites()
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
   private void setSocketModes( SSLSocket sock )
   {
      if ( theBuilder != null )
      {
         sock.setUseClientMode( theBuilder.isSocketUseClientMode() );

         if ( theBuilder.isClientAuthModeWant() )
         {
            sock.setNeedClientAuth( false );
            sock.setWantClientAuth( true );
         }
         else if ( theBuilder.isClientAuthModeNeed() )
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