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

package org.jboss.remoting;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.jboss.logging.Logger;
import org.jboss.remoting.serialization.SerializationStreamFactory;
import org.jboss.remoting.transport.ClientInvoker;
import org.jboss.remoting.util.SecurityUtility;

/**
 * InvokerLocator is an object that indentifies a specific Invoker on the network, via a unique
 * locator URI. The locator URI is in the format: <P>
 * <p/>
 * <tt>protocol://host[:port][/path[?param=value&param2=value2]]</tt>     <P>
 * <p/>
 * For example, a http based locator might be: <P>
 * <p/>
 * <tt>http://192.168.10.1:8081</tt>  <P>
 * <p/>
 * An example Socket based locator might be: <P>
 * <p/>
 * <tt>socket://192.168.10.1:9999</tt>  <P>
 * <p/>
 * An example RMI based locator might be: <P>
 * <p/>
 * <tt>rmi://localhost</tt>  <P>
 * <p/>
 * NOTE: If no hostname is given (e.g., "socket://:5555"), then the hostname will
 * automatically be resolved to the outside IP address of the local machine.  
 * If the given hostname is 0.0.0.0 and the system
 * property "jboss.bind.address" is set, then the hostname will be replaced by the value
 * associated with 'jboss.bind.address".
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @version $Revision: 5885 $
 */
public class InvokerLocator implements Serializable
{
   static final long serialVersionUID;
   protected static Logger log = Logger.getLogger(InvokerLocator.class); 
   protected static Boolean legacyParsingFlag;

   protected String protocol;
   protected String host;
   protected ArrayList connectHomes = new ArrayList();
   protected ArrayList homes = new ArrayList();
   protected int port;
   protected String path;
   protected String query;
   protected Map parameters;
   private String uri;
   private String originalURL;
   private Home homeInUse;


   static
   {
      if(Version.getDefaultVersion() == Version.VERSION_1)
      {
         serialVersionUID = -2909329895029296248L;
      }
      else
      {
         serialVersionUID = -4977622166779282521L;
      }
   }
   
   public static boolean getUseLegacyParsing()
   {
      if (legacyParsingFlag == null)
         return false;
      return legacyParsingFlag.booleanValue();
   }
   
   public static void setUseLegacyParsing(boolean flag)
   {
      legacyParsingFlag = new Boolean(flag);
   }

   /**
    * Indicates should address binding to all network interfaces (i.e. 0.0.0.0)
    */
   public static final String ANY = "0.0.0.0";
   /**
    * Constant value for server bind address system property.  Value is 'jboss.bind.address'.
    */
   private static final String SERVER_BIND_ADDRESS = "jboss.bind.address";

   /**
    * Public key to use when want to specify that locator look up local address by
    * IP or host name.
    */
   public static final String BIND_BY_HOST = "remoting.bind_by_host";


   /**
    * Constant to define the param name to be used when defining the data type.
    */
   public static final String DATATYPE = "datatype";
   public static final String DATATYPE_CASED = "dataType";

   /**
    * Constant to define the param name to be used when defining the serialization type.
    */
   public static final String SERIALIZATIONTYPE = "serializationtype";
   public static final String SERIALIZATIONTYPE_CASED = "serializationType";

   /**
    * Constant to define the param name to be used when defining the marshaller fully qualified classname
    */
   public static final String MARSHALLER = "marshaller";
   /**
    * Constant to define the param name to be used when defining the unmarshaller fully qualified classname
    */
   public static final String UNMARSHALLER = "unmarshaller";

   /**
    * Constant to define what port the marshalling loader port resides on.
    */
   public static final String LOADER_PORT = "loaderport";

   /**
    * Constant to define the param name to be used when defining if marshalling should be by value,
    * which means will be using local client invoker with cloning of payload value.
    */
   public static final String BYVALUE = "byvalue";

   /**
    * Constant to define the param name to be used when defining if marshalling should use
    * remote client invoker instead of using local client invoker (even though both client and
    * server invokers are within same JVM).
    */
   public static final String FORCE_REMOTE = "force_remote";   

   /**
    * Constant to define if client should try to automatically establish a
    * lease with the server.  Value for this parameter key should be either 'true' or 'false'.
    */
   public static final String CLIENT_LEASE = "leasing";

   /**
    * Constant to define what the client lease period should be in the case that
    * server side leasing is turned on.  Value for this parameter key should be the number
    * of milliseconds to wait before each client lease renewal and must be greater than zero
    * in order to be recognized.
    */
   public static final String CLIENT_LEASE_PERIOD = "lease_period";

   
   /**
    * Constant to define if InvokerLocator should use the old (ad hoc) parsing
    * algorithm instead of the new, URI based, parsing algorithm.
    */
   public static final String LEGACY_PARSING = "legacyParsing";
   
   /**
    * Serves as placeholder in host position when multiple hosts are given in the
    * query part by way of the parameter "hosts".  E.g.
    * <p>
    * socket://multihome:8888/?hosts=host1.jboss.org:host2.jboss.org
    */
   public static final String MULTIHOME = "multihome";
   
   /**
    * Parameter key used for specifying multiple homes to connect to.  E.g.
    * <p>
    * socket://multihome/?connecthomes=host1.jboss.org:7777!host2.jboss.org:8888
    */
   public static final String CONNECT_HOMES_KEY = "connecthomes";
   
   /**
    * Parameter key used for specifying multiple binding homes.  E.g.
    * <p>
    * socket://multihome/?homes=a.org:66!b.org:77&homes=c.org:88!d.org:99
    */
   public static final String HOMES_KEY = "homes";
   
   /**
    * Parameter key used for specifying default connecting port for
    * multihome InvokerLocator.
    */
   public static final String DEFAULT_CONNECT_PORT = "defaultConnectPort";
   
   /**
    * Parameter key used for specifying default server bind port for
    * multihme InvokerLocator.
    */
   public static final String DEFAULT_PORT = "defaultPort";
   
   /**
    * Constant to determine if warning about null host should be logged.
    */
   public static final String SUPPRESS_HOST_WARNING = "suppressHostWarning";
   
   /**
    * InvokerLocator leaves address 0.0.0.0 unchanged.  Once serverBindAddress has been
    * extracted from the InvokerLocator, it is necessary to transform 0.0.0.0 into an
    * address that contacted over the network.  See JBREM-687.
    */
   public static InvokerLocator validateLocator(InvokerLocator locator) throws MalformedURLException
   {
      InvokerLocator externalLocator = locator;

      String host = locator.getHost();
      String newHost = null;
      if(host == null || InvokerLocator.ANY.equals(host))
      {
         // now need to get some external bindable address
         try
         {
            newHost = (String)AccessController.doPrivileged( new PrivilegedExceptionAction()
            {
               public Object run() throws Exception
               {
                  String bindByHost = System.getProperty(InvokerLocator.BIND_BY_HOST, "True");
                  boolean byHost = Boolean.valueOf(bindByHost).booleanValue();
                  if(byHost)
                  {
                     return InetAddress.getLocalHost().getHostName();
                  }
                  else
                  {
                     return InetAddress.getLocalHost().getHostAddress();
                  }
               }
            });
         }
         catch (PrivilegedActionException e)
         {
            log.debug("Could not get host by name or address.", e.getCause());
         }

         if(newHost == null)
         {
            // now what?  step through network interfaces?
            throw new RuntimeException("Can not determine bindable address for locator (" + locator + ")");
         }
         
         externalLocator = new InvokerLocator(locator.protocol, newHost, locator.port,
                                              locator.getPath(), locator.getParameters());
      }

      return externalLocator;
   }
   
   
   public static void extractHomes(String homeList, List list, int defaultPort)
   {
      StringTokenizer tok = new StringTokenizer(homeList, "!");  
      while(tok.hasMoreTokens())
      {
         String h = null;
         int p = -1;
         String token = tok.nextToken();
         boolean isIPv6 = token.indexOf('[') >= 0;
         int boundary;

         if (isIPv6)
         {
            int pos = token.indexOf(']');
            if (pos + 1 == token.length())
               boundary = -1;
            else
               boundary = token.indexOf(']') + 1;
            
         }
         else
         {
            boundary = token.lastIndexOf(':');
         }

         if (boundary > -1)
         {
            h = token.substring(0, boundary);

            try
            {
               p = Integer.valueOf(token.substring(boundary + 1)).intValue();
            }
            catch (NumberFormatException  e)
            {
               log.warn("invalid port value: " + token.substring(boundary + 1));
            }
         }
         else
         {
            h = token;
         }

         if (p == -1)
            p = defaultPort;

         list.add(new Home(h, p));
      }
   }
   
   
   public static String convertHomesListToString(List homes)
   {
      if (homes == null || homes.size() == 0)
         return "";
      
      Iterator it = homes.iterator();
      StringBuffer b = new StringBuffer(((Home) it.next()).toString());
      while (it.hasNext())
      {
         b.append("!").append(it.next());
      }
      return b.toString();
   }
   
   
   /**
    * Constructs the object used to identify a remoting server via simple uri format string (e.g. socket://myhost:7000).
    * Note: the uri passed may not always be the one returned via call to getLocatorURI() as may need to change if
    * port not specified, host is 0.0.0.0, etc.  If need original uri that is passed to this constructor, need to
    * call getOriginalURI().
    * @param uri
    * @throws MalformedURLException
    */
   public InvokerLocator(String uri)
         throws MalformedURLException
   {
      originalURL = uri;
      parse(originalURL);
   }
   
   
   private void parse(String uriString) throws MalformedURLException
   {
      boolean doLegacyParsing = false;
      if (legacyParsingFlag != null)
      {
         doLegacyParsing = legacyParsingFlag.booleanValue();
      }
      else 
      {
         String s = getSystemProperty(LEGACY_PARSING);
         doLegacyParsing = "true".equalsIgnoreCase(s);
      }

      if (doLegacyParsing)
      {
         log.warn("using deprecated legacy URL parsing routine");
         legacyParse(uriString);
      }
      else
      {
         URIParse(uriString);
      }
      
      if (query != null)
      {
         StringTokenizer tok = new StringTokenizer(query, "&");
         parameters = new TreeMap();
         while(tok.hasMoreTokens())
         {
            String token = tok.nextToken();
            int eq = token.indexOf("=");
            String name = (eq > -1) ? token.substring(0, eq) : token;
            String value = (eq > -1) ? token.substring(eq + 1) : "";
            parameters.put(name, value);
         }
      }

      if (!MULTIHOME.equals(host) && parameters != null)
      {
         // Use "host:port" to connect.
         String s = (String) parameters.remove(CONNECT_HOMES_KEY);
         if (s != null) log.warn("host != " + MULTIHOME + ": " + CONNECT_HOMES_KEY + " will be ignored");
      }

      if (parameters != null)
      {
         String homesString = (String) parameters.remove(HOMES_KEY);
         String connectHomesString = (String) parameters.remove(CONNECT_HOMES_KEY);
         createHomeLists(parameters, homesString, connectHomesString);
      }

//    rebuild it, since the host probably got resolved and the port changed
      rebuildLocatorURI();
      
      if (!MULTIHOME.equals(host))
      {
         homeInUse = new Home(host, port);
         connectHomes.add(homeInUse);
         if (homes.isEmpty())
            homes.add(homeInUse);
      }
   }

   private void URIParse(String uriString) throws MalformedURLException
   {
      try
      {
         URI uri = new URI(encodePercent(uriString));
         protocol = uri.getScheme();
         checkHost(originalURL, uri.getHost());
         host = decodePercent(resolveHost(uri.getHost()));
         port = uri.getPort();
         path = uri.getPath();
         query = decodePercent(uri.getQuery());
      }
      catch (URISyntaxException e)
      {
         throw new MalformedURLException(e.getMessage());
      }
   }
   
   private void legacyParse(String uri) throws MalformedURLException
   {
      log.warn("Legacy InvokerLocator parsing is deprecated");
      int i = uri.indexOf("://");
      if(i < 0)
      {
         throw new MalformedURLException("Invalid url " + uri);
      }
      String tmp = uri.substring(i + 3);
      this.protocol = uri.substring(0, i);
      i = tmp.indexOf("/");
      int p = tmp.indexOf(":");
      if(i != -1)
      {
         p = (p < i ? p : -1);
      }
      if(p != -1)
      {
         host = resolveHost(tmp.substring(0, p).trim());
         if(i > -1)
         {
            port = Integer.parseInt(tmp.substring(p + 1, i));
         }
         else
         {
            port = Integer.parseInt(tmp.substring(p + 1));
         }
      }
      else
      {
         if(i > -1)
         {
            host = resolveHost(tmp.substring(0, i).trim());
         }
         else
         {
            host = resolveHost(tmp.substring(0).trim());
         }
         port = -1;
      }

      // now look for any path
      p = tmp.indexOf("?");
      if(p != -1)
      {
         path = tmp.substring(i + 1, p);
         query = tmp.substring(p + 1);
      }
      else
      {
         p = tmp.indexOf("/");
         if(p != -1)
         {
            path = tmp.substring(p + 1);
         }
         else
         {
            path = "";
         }
         query = null;
      }
   }

   private static void checkHost(String uri, String host)
   {
      if (host == null && !getBoolean(SUPPRESS_HOST_WARNING))
      {
         StringBuffer sb = new StringBuffer("Host resolves to null in ");
         sb.append(uri).append(". Perhaps the host contains an invalid character.  ");
         sb.append("See http://www.ietf.org/rfc/rfc2396.txt.");
         log.warn(sb.toString());
      }
   }
   
   private static final String resolveHost(String host)
   {
      if (host == null)
      {
         host = fixRemoteAddress(host);
      }
      else if(host.indexOf("0.0.0.0") != -1)
      {
         String bindAddress = getSystemProperty(SERVER_BIND_ADDRESS, "0.0.0.0");         
         if(bindAddress.equals("0.0.0.0"))
         {
            host = fixRemoteAddress(host);
         }
         else
         {
            host = host.replaceAll("0\\.0\\.0\\.0", getSystemProperty(SERVER_BIND_ADDRESS));
         }
      }
      return host;
   }

   private static String fixRemoteAddress(String address)
   {
      if(address == null)
      {
         try
         {
            address = (String)AccessController.doPrivileged( new PrivilegedExceptionAction()
            {
               public Object run() throws UnknownHostException
               {
                  String bindByHost = System.getProperty(BIND_BY_HOST, "True");
                  boolean byHost = Boolean.valueOf(bindByHost).booleanValue();

                  if(byHost)
                  {
                     return InetAddress.getLocalHost().getHostName();
                  }
                  else
                  {
                     return InetAddress.getLocalHost().getHostAddress();
                  }
               }
            });
         }
         catch (PrivilegedActionException e)
         {
            log.debug("error", e.getCause());
         }
      }

      return address;
   }
  

   private void createHomeLists(Map parameters, String homesString, String connectHomesString)
   {
      // DEFAULT_PORT value takes precedence, followed by port value.
      int defaultPort = port; 
      String s = (String) parameters.get(DEFAULT_PORT);
      if (s != null && s != "")
      {
         try
         {
            defaultPort = Integer.parseInt(s);
         }
         catch (Exception e)
         {
            log.warn("invalid value for " + DEFAULT_PORT + ": " + s);
         }
      }
      
      if (homesString != null)
      {
         extractHomes(homesString, homes, defaultPort);
      }
      
      // DEFAULT_CONNECT_PORT value takes precedence, followed by 
      // DEFAULT_PORT value and then port value.
      s = (String) parameters.get(DEFAULT_CONNECT_PORT);
      if (s != null && s != "")
      {
         try
         {
            defaultPort = Integer.parseInt(s);
         }
         catch (Exception e)
         {
            log.warn("invalid value for " + DEFAULT_CONNECT_PORT + ": " + s);
         }
      }

      if (connectHomesString != null)
      {
         extractHomes(connectHomesString, connectHomes, defaultPort);
      }
   }
   
   
   /**
    * Constructs the object used to identify a remoting server.
    * @param protocol
    * @param host
    * @param port
    * @param path
    * @param parameters
    */
   public InvokerLocator(String protocol, String host, int port, String path, Map parameters)
   {
      this.protocol = protocol;
      this.host = resolveHost(host);
      this.port = port;
      this.path = path == null ? "" : path;
      this.parameters = parameters == null ? new TreeMap() : new TreeMap(parameters);
     
      if (this.parameters != null)
      {
         String homesString = (String) this.parameters.remove(HOMES_KEY);
         String connectHomesString = (String) this.parameters.remove(CONNECT_HOMES_KEY);
         createHomeLists(this.parameters, homesString, connectHomesString);
      }
      
      // Test for IPv6 host address.
      if (this.host != null && this.host.indexOf(":") >= 0 && this.host.indexOf("[") == -1)
      {
         this.host = "[" + this.host + "]";
      }
      
      rebuildLocatorURI();
      originalURL = uri;
      
      if (!MULTIHOME.equals(host))
      {
         homeInUse = new Home(host, port);
         homes.add(homeInUse);
         connectHomes.add(homeInUse);

         if (parameters != null)
         {
            // Use "host:port" to connect.
            String s = (String) parameters.remove(CONNECT_HOMES_KEY);
            if (s != null) log.warn("host != " + MULTIHOME + ": " + CONNECT_HOMES_KEY + " will be ignored");
         }
      }
   }

   public int hashCode()
   {
      return uri.hashCode();
   }

   /**
    * Compares to see if Object passed is of type InvokerLocator and
    * it's internal locator uri hashcode is same as this one.  Note, this
    * means is testing to see if not only the host, protocol, and port are the
    * same, but the path and parameters as well.  Therefore 'socket://localhost:9000'
    * and 'socket://localhost:9000/foobar' would NOT be considered equal.
    * @param obj
    * @return
    */
   public boolean equals(Object obj)
   {
      return obj != null && obj instanceof InvokerLocator && uri.equals(((InvokerLocator)obj).getLocatorURI());
   }

   /**
    * Compares to see if InvokerLocator passed represents the same physical remoting server
    * endpoint as this one.  Unlike the equals() method, this just tests to see if protocol, host,
    * and port are the same and ignores the path and parameters.
    * @param compareMe
    * @return
    */
   public boolean isSameEndpoint(InvokerLocator compareMe)
   {
      return compareMe != null && getProtocol().equalsIgnoreCase(compareMe.getProtocol()) &&
             getHost().equalsIgnoreCase(compareMe.getHost()) && getPort() == compareMe.getPort();
   }
   
   public boolean isCompatibleWith(InvokerLocator other)
   {
      if (other == null)
         return false;
      
      // If this or other comes from pre-2.4.0 Remoting.
      if (homes == null || other.homes == null)
         return false;
      
      boolean result1 = getProtocol().equalsIgnoreCase(other.getProtocol()) &&
                        path.equals(other.getPath()) &&
                        parameters.equals(other.getParameters());
      
      ArrayList tempHomes = connectHomes.isEmpty() ? homes : connectHomes;
      boolean result2 = intersects(tempHomes, other.getConnectHomeList()) || 
                        intersects(tempHomes, other.getHomeList());
      
      return result1 && result2;
   }

   /**
    * return the locator URI, in the format: <P>
    * <p/>
    * <tt>protocol://host[:port][/path[?param=value&param2=value2]]</tt>
    * Note, this may not be the same as the original uri passed as parameter to the constructor.
    * @return
    */
   public String getLocatorURI()
   {
      return uri;
   }

   public String getProtocol()
   {
      return protocol;
   }

   public String getHost()
   {
      if (host.equals(MULTIHOME) && homeInUse != null)
         return homeInUse.host;
      
      return host;
   }
   
   public String getActualHost()
   {
      return host;
   }
   
   public boolean isMultihome()
   {
      return MULTIHOME.equals(host);
   }
   
   public String getConnectHomes()
   {
      return convertHomesListToString(connectHomes);
   }
   
   public List getConnectHomeList()
   {
      if (connectHomes == null)
      {
         ArrayList list = new ArrayList();
         list.add(new Home(host, port));
         return list;
      }
      
      return new ArrayList(connectHomes);
   }
   
   public void setConnectHomeList(List connectHomes)
   {
      if (connectHomes == null)
         this.connectHomes = new ArrayList();
      else
         this.connectHomes = new ArrayList(connectHomes);
      
      rebuildLocatorURI();
   }
   
   public Home getHomeInUse()
   {
      if (homeInUse != null || isMultihome())
         return homeInUse;
      
      return new Home(host, port);
   }
   
   public void setHomeInUse(Home homeInUse)
   {
      this.homeInUse = homeInUse;
   }
   
   public String getHomes()
   {  
      return convertHomesListToString(homes);
   }
   
   public List getHomeList()
   {
      if (homes == null)
      {
         ArrayList list = new ArrayList();
         list.add(new Home(host, port));
         return new ArrayList();
      }
      
      return new ArrayList(homes);
   }

   public void setHomeList(List homes)
   {
      if (homes == null)
         this.homes = new ArrayList();
      else
         this.homes = new ArrayList(homes);
      
      rebuildLocatorURI();
   }
   
   public int getPort()
   {
      if (host.equals(MULTIHOME) && homeInUse != null)
         return homeInUse.port;
      
      return port;
   }
   
   public int getActualPort()
   {
      return port;
   }

   public String getPath()
   {
      return path;
   }

   public Map getParameters()
   {
      if (parameters == null)
      {
         parameters = new TreeMap();
      }
      return parameters;
   }

   public String toString()
   {
      return "InvokerLocator [" + uri + "]";
   }

   /**
    * Gets the original uri passed to constructor (if there was one).
    * @return
    */
   public String getOriginalURI()
   {
      return originalURL;
   }

   /**
    * narrow this invoker to a specific RemoteClientInvoker instance
    *
    * @return
    * @throws Exception
    */
   public ClientInvoker narrow() throws Exception
   {  
      try
      {
         return (ClientInvoker) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return InvokerRegistry.createClientInvoker(InvokerLocator.this);
            }
         });
      }
      catch (PrivilegedActionException pae)
      {
         throw pae.getException();
      }
   }


   public String findSerializationType()
   {
      String serializationTypeLocal = SerializationStreamFactory.JAVA;
      if(parameters != null)
      {
         serializationTypeLocal = (String) parameters.get(SERIALIZATIONTYPE);
         if(serializationTypeLocal == null)
         {
            serializationTypeLocal = (String) parameters.get(InvokerLocator.SERIALIZATIONTYPE_CASED);
            if(serializationTypeLocal == null)
            {
               serializationTypeLocal = SerializationStreamFactory.JAVA;
            }
         }
      }

      return serializationTypeLocal;
   }

   protected boolean intersects(Collection c1, Collection c2)
   {
      Iterator it = c1.iterator();
      while (it.hasNext())
      {
         if (c2.contains(it.next()))
            return true;
      }
      return false;
   }
   
   protected void rebuildLocatorURI()
   {  
      String portPart = (port > -1) ? (":" + port) : "";
      String divider = path.startsWith("/") ? "" : "/";
      String parametersPart = (parameters != null) ? "?" : "";
      uri = protocol + "://" + host + portPart + divider + path + parametersPart;
      
      if(parameters != null)
      {
         if (!homes.isEmpty())
            parameters.put(HOMES_KEY, convertHomesListToString(homes));
         if (!connectHomes.isEmpty())
            parameters.put(CONNECT_HOMES_KEY, convertHomesListToString(connectHomes));
         
         Iterator iter = parameters.keySet().iterator();
         while(iter.hasNext())
         {
            String key = (String) iter.next();
            String val = (String) parameters.get(key);
            if ("".equals(val))
            {
               uri += key;  
            }
            else
            {
               uri += key + "=" + val;  
            }
            if(iter.hasNext())
            {
               uri += "&";
            }
         }
         
         parameters.remove(HOMES_KEY);
         parameters.remove(CONNECT_HOMES_KEY);
      }
   }
   
   protected static String encodePercent(String s)
   {
      if (s == null) return null;
      StringTokenizer st = new StringTokenizer(s, "%");
      StringBuffer sb = new StringBuffer();
      int limit = st.countTokens() - 1;
      for (int i = 0; i < limit; i++)
      {
         String token = st.nextToken();
         sb.append(token).append("%25");
      }
      sb.append(st.nextToken());
      return sb.toString();
   }
   
   protected static String decodePercent(String s)
   {
      if (s == null) return null;
      StringBuffer sb = new StringBuffer();
      int fromIndex = 0;
      int index = s.indexOf("%25", fromIndex);
      while (index >= 0)
      {
         sb.append(s.substring(fromIndex, index)).append('%');
         fromIndex = index + 3;
         index = s.indexOf("%25", fromIndex);
      }
      sb.append(s.substring(fromIndex));
      return sb.toString();
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
   
   static private boolean getBoolean(final String name)
   {
      if (SecurityUtility.skipAccessControl())
         return Boolean.getBoolean(name);
      
      Boolean value = null;
      try
      {
         value = (Boolean)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return Boolean.valueOf(Boolean.getBoolean(name));
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RuntimeException) e.getCause();
      }
      
      return value.booleanValue();
   }
}
