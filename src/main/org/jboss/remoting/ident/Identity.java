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
package org.jboss.remoting.ident;

import org.jboss.remoting.network.NetworkRegistry;
import org.jboss.remoting.util.SecurityUtility;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.dgc.VMID;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

/**
 * Identity is used to uniquely identify a JBoss server instance on the network.
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @version $Revision: 5002 $
 */
public class Identity implements Serializable
{
   static final long serialVersionUID = -2788084303665751253L;

   private static transient Random random = new Random(System.currentTimeMillis());
   public static transient String DEFAULT_DOMAIN = "JBOSS";
   private static transient String _domain = DEFAULT_DOMAIN;
   
   static
   {
      _domain = getSystemProperty("jboss.identity.domain", DEFAULT_DOMAIN);
   }
   
   private static transient Map identities = new WeakHashMap(2);

   private final String instanceid;
   private final InetAddress ip;
   private final String serverid;
   private String domain;
   private int hashCode;

   private Identity(InetAddress addr, String instanceid, String serverid)
   {
      this.ip = addr;
      this.instanceid = instanceid;
      this.serverid = serverid;
      this.domain = ((_domain == null || _domain.equals("")) ? DEFAULT_DOMAIN : _domain);
      calcHashCode();
   }

   private void calcHashCode()
   {
      this.hashCode = (ip.hashCode() + instanceid.hashCode() + serverid.hashCode() - domain.hashCode());
   }

   /**
    * set the domain for all active mbean servers
    */
   public static void setDomain(String domain)
   {
      Iterator iter = identities.keySet().iterator();
      while(iter.hasNext())
      {
         Identity ident = (Identity) identities.get(iter.next());
         if(ident != null)
         {
            ident.domain = domain;
         }
         ident.calcHashCode();
      }
      setSystemProperty("jboss.identity.domain", domain);
      _domain = domain;
      NetworkRegistry.getInstance().changeDomain(domain);
   }


   public int hashCode()
   {
      return hashCode;
   }


   public String toString()
   {
      return "JBOSS Identity\n\taddress:" + ip + "\n\tinstanceid:" + instanceid + "\n\tJMX id:" + serverid + "\n\tdomain:" + domain + "\n";
   }

   /**
    * return the domain for the server
    *
    * @return
    */
   public final String getDomain()
   {
      return domain;
   }

   /**
    * return the JBOSS Instance ID, which is the same between reboots of the same
    * JBOSS server instance.
    *
    * @return
    */
   public String getInstanceId()
   {
      return this.instanceid;
   }

   /**
    * return the JBOSS IP Address of the instance
    *
    * @return
    */
   public InetAddress getAddress()
   {
      return this.ip;
   }

   /**
    * return the JMX server ID for the JBoss instance, which is different between
    * reboots of the same JBOSS server instance.
    *
    * @return
    */
   public String getJMXId()
   {
      return this.serverid;
   }

   /**
    * returns true if the identity represents the same JVM
    *
    * @param identity
    * @return
    */
   public boolean isSameJVM(Identity identity)
   {
      return identity.equals(this);
   }

   /**
    * returns true if the identity represents the same JBOSS Instance, although
    * may not be the exact same process space since the JVM process space might be
    * different (such as a reboot).
    *
    * @param identity
    * @return
    */
   public boolean isSameInstance(Identity identity)
   {
      return identity.getInstanceId().equals(instanceid);
   }

   /**
    * returns true if the identity is on the same JBOSS machine, represented by the
    * same IP address (this may not work in the case several physically different
    * machines have the same IP Address).
    *
    * @param identity
    * @return
    */
   public boolean isSameMachine(Identity identity)
   {
      return identity.getAddress().equals(ip);
   }

   public boolean equals(Object obj)
   {
      if(obj instanceof Identity)
      {
         return hashCode == obj.hashCode();
      }
      return false;
   }

   public static synchronized final Identity get(final MBeanServer server)
   {
      if(identities.containsKey(server))
      {
         return (Identity) identities.get(server);
      }
      try
      {
         InetAddress localHost = getLocalHost();
         ObjectName objectName = new ObjectName("JMImplementation:type=MBeanServerDelegate");
         String serverid = (String) getMBeanAttribute(server, objectName, "MBeanServerId");
         Identity identity = new Identity(localHost, createId(server), serverid);
         identities.put(server, identity);
         return identity;
      }
      catch(Exception ex)
      {
         String type = ex.getClass().getName();
         final RuntimeException rex = new RuntimeException("Exception creating identity: " + type + ": " + ex.getMessage());
         rex.setStackTrace(ex.getStackTrace());
         throw rex;
      }
   }

   private static final synchronized String createId(final MBeanServer server)
   {
      // we can set as a system property
      String myid = getSystemProperty("jboss.identity");
      if(myid != null)
      {
         return myid;
      }
      String id = null;
      File file = null;
      try
      {
         // FIRST TRY THE JBOSS guy to determine our data directory
         final ObjectName obj = new ObjectName("jboss.system:type=ServerConfig");
         File dir = (File) getMBeanAttribute(server, obj, "ServerDataDir");
         if(dir != null)
         {
            if(fileExists(dir) == false)
            {
               mkdirs(dir);
            }
            file = new File(dir, "jboss.identity");
         }
      }
      catch(Exception ex)
      {
      }
      if(file == null)
      {
         // we may not have that mbean, which is OK
         String fl = getSystemProperty("jboss.identity.dir", ".");
         
         File dir = new File(fl);
         if(fileExists(dir) == false)
         {
            mkdirs(dir);
         }
         file = new File(dir, "jboss.identity");
      }

      boolean canRead = canRead(file);
      if(fileExists(file) && canRead)
      {
         InputStream is = null;
         try
         {
            is = getFileInputStream(file);
            byte buf[] = new byte[800];
            int c = is.read(buf);
            id = new String(buf, 0, c);
         }
         catch(Exception ex)
         {
            throw new RuntimeException("Error loading jboss.identity: " + ex.toString());
         }
         finally
         {
            if(is != null)
            {
               try
               {
                  is.close();
               }
               catch(Exception ig)
               {
               }
            }
         }
      }
      else
      {
         OutputStream out = null;
         try
         {
            id = createUniqueID();
            if(fileExists(file) == false)
            {
               createNewFile(file);
            }

            out = getFileOutputStream(file);
            out.write(id.getBytes());
         }
         catch(Exception ex)
         {
            throw new RuntimeException("Error creating Instance ID: " + ex.toString());
         }
         finally
         {
            if(out != null)
            {
               try
               {
                  out.flush();
                  out.close();
               }
               catch(Exception ig)
               {
               }
            }
         }
      }
      
      setSystemProperty("jboss.identity", id);
      
      return id;
   }

   public static final String createUniqueID()
   {
      String id = new VMID().toString();
      // colons don't work in JMX
      return id.replace(':', 'x') + random.nextInt(1000);
   }
   
   static private boolean fileExists(final File file)
   {
      if (file == null)
         return false;
      
      if (SecurityUtility.skipAccessControl())
      {
         return file.exists();
      }

      return ((Boolean)AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            return new Boolean(file.exists());
         }
      })).booleanValue();
   }
   
   static private boolean mkdirs(final File dir)
   {
      if (SecurityUtility.skipAccessControl())
      {
         return dir.mkdirs();
      }
      
      return ((Boolean) AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            return new Boolean(dir.mkdirs());
         }
      })).booleanValue();
   }
   
   static private FileInputStream getFileInputStream(final File file) throws FileNotFoundException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return new FileInputStream(file);
      }
      
      try
      {
         return (FileInputStream)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws FileNotFoundException
            {
               return new FileInputStream(file);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (FileNotFoundException) e.getCause();
      }
   }
   
   static private FileOutputStream getFileOutputStream(final File file)
   throws FileNotFoundException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return new FileOutputStream(file);
      }
      
      try
      {
         return (FileOutputStream)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws FileNotFoundException
            {
               return new FileOutputStream(file);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (FileNotFoundException) e.getCause();
      }
   }
   
   static private boolean canRead(final File file)
   {
      if (SecurityUtility.skipAccessControl())
      {
         return file.canRead();
      }
      
      return ((Boolean)AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            return new Boolean(file.canRead());
         }
      })).booleanValue();
   }
   
   static private boolean createNewFile(final File file) throws IOException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return file.createNewFile();
      }
      
      try
      {
         return ((Boolean)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return new Boolean(file.createNewFile());
            }
         })).booleanValue();
      }
      catch (Exception e)
      {
         throw (IOException) e.getCause();
      }
   }
   
   static private Object getMBeanAttribute(final MBeanServer server, final ObjectName objectName, final String attribute)
   throws Exception
   {
      if (SecurityUtility.skipAccessControl())
      {
         return server.getAttribute(objectName, attribute);
      }
      
      try
      {
         return AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return server.getAttribute(objectName, attribute);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (Exception) e.getCause();
      }  
   }
   
   static private String getSystemProperty(final String name, final String defaultValue)
   {
      if (SecurityUtility.skipAccessControl())
         return System.getProperty(name, defaultValue);
         
      String value = null;
      try
      {
         value = (String)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return System.getProperty(name, defaultValue);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RuntimeException) e.getCause();
      }
      
      return value;
   }
   
   static private String getSystemProperty(final String name)
   {
      if (SecurityUtility.skipAccessControl())
         return System.getProperty(name);
      
      String value = null;
      try
      {
         value = (String)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return System.getProperty(name);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RuntimeException) e.getCause();
      }
      
      return value;
   }
   
   static private void setSystemProperty(final String name, final String value)
   {
      if (SecurityUtility.skipAccessControl())
      {
         System.setProperty(name, value);
         return;
      }
      
      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return System.setProperty(name, value);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RuntimeException) e.getCause();
      }
   }
   
   static private InetAddress getLocalHost() throws UnknownHostException
   {
      if (SecurityUtility.skipAccessControl())
      {
         try
         {
            return InetAddress.getLocalHost();
         }
         catch (IOException e)
         {
            return InetAddress.getByName("127.0.0.1");
         }
      }

      try
      {
         return (InetAddress) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IOException
            {
               try
               {
                  return InetAddress.getLocalHost();
               }
               catch (IOException e)
               {
                  return InetAddress.getByName("127.0.0.1");
               }
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (UnknownHostException) e.getCause();
      }
   }
}
