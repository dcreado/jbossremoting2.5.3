/*
* JBoss, Home of Professional Open Source
* Copyright 2005, JBoss Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.remoting.util.socket;

import java.security.PrivateKey;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.net.Socket;
import javax.net.ssl.X509KeyManager;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class RemotingKeyManager implements X509KeyManager
{
   private X509KeyManager targetManager = null;
   private String alias = null;

   public RemotingKeyManager(X509KeyManager x509KeyManager, String alias)
   {
      this.targetManager = x509KeyManager;
      this.alias = alias;
   }

   public PrivateKey getPrivateKey(String string)
   {
      return targetManager.getPrivateKey(string);
   }

   public X509Certificate[] getCertificateChain(String string)
   {
      return targetManager.getCertificateChain(string);
   }

   public String[] getClientAliases(String string, Principal[] principals)
   {
      return targetManager.getClientAliases(string, principals);
   }

   public String[] getServerAliases(String string, Principal[] principals)
   {
      return targetManager.getServerAliases(string, principals);
   }

   public String chooseServerAlias(String string, Principal[] principals, Socket socket)
   {
      return targetManager.chooseServerAlias(string, principals, socket);
   }

   public String chooseClientAlias(String[] strings, Principal[] principals, Socket socket)
   {
      // just returning supplied alias instead of searching
      return alias;
   }
}