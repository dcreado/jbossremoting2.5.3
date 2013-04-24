package org.jboss.test.remoting.transport.rmi.ssl.config;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

interface SerializableServerSocketFactoryMBean
{

   public abstract ServerSocket createServerSocket(int arg0) throws IOException;

   public abstract ServerSocket createServerSocket(int arg0, int arg1) throws IOException;

   public abstract ServerSocket createServerSocket(int arg0, int arg1, InetAddress arg2) throws IOException;

}