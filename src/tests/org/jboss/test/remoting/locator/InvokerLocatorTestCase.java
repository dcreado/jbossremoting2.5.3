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

package org.jboss.test.remoting.locator;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.util.List;

import junit.framework.TestCase;

import org.jboss.logging.Logger;
import org.jboss.remoting.Home;
import org.jboss.remoting.InvokerLocator;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class InvokerLocatorTestCase extends TestCase
{
   protected Logger log = Logger.getLogger(InvokerLocatorTestCase.class);
   
   public void testLegacyParsingFlag() throws Exception
   {
      // Test no legacy flag configuration.
      InvokerLocator locator = new InvokerLocator("http://localhost:1234/services");
      assertEquals("/services", locator.getPath());
      
      // Test setting legacy parsing by System property
      System.setProperty(InvokerLocator.LEGACY_PARSING, "true");
      locator = new InvokerLocator("http://localhost:1234/services");
      assertEquals("services", locator.getPath());
      
      // Test setting legacy parsing by InvokerLocator static field.
      System.setProperty(InvokerLocator.LEGACY_PARSING, "");
      InvokerLocator.setUseLegacyParsing(true);
      locator = new InvokerLocator("http://localhost:1234/services");
      assertEquals("services", locator.getPath());
      
      // Test that static field overrides System property.
      InvokerLocator.setUseLegacyParsing(true);
      System.setProperty(InvokerLocator.LEGACY_PARSING, "false");
      locator = new InvokerLocator("http://localhost:1234/services");
      assertEquals("services", locator.getPath());
      
      // Test that static field overrides System property.
      InvokerLocator.setUseLegacyParsing(false);
      System.setProperty(InvokerLocator.LEGACY_PARSING, "true");
      locator = new InvokerLocator("http://localhost:1234/services");
      assertEquals("/services", locator.getPath());
   }
   
   /**
    *  testLegacyLocatorConfig tests the original version of InvokerLocator parsing.
    */
   public void testLegacyLocatorConfig() throws Exception
   {
      System.setProperty(InvokerLocator.LEGACY_PARSING, "true");
      InvokerLocator locator = new InvokerLocator("http://localhost:1234/services/uri:Test");
      InvokerLocator locator2 = new InvokerLocator("http://localhost:1234");
      InvokerLocator locator3 = new InvokerLocator("http://127.0.0.1:1234");
      InvokerLocator locator4 = new InvokerLocator("http://1.2.3.4/aaa");
      InvokerLocator locator5 = new InvokerLocator("http://1.2.3.4/abB");

      assertFalse(locator.equals(null));
      assertFalse(locator.equals(locator2));
      assertFalse(locator.isSameEndpoint(null));
      assertTrue(locator.isSameEndpoint(locator2));
      assertFalse(locator.equals(locator3));
      assertFalse(locator.isSameEndpoint(locator3));

      assertTrue(locator4.equals(locator4));
      assertFalse(locator4.equals(locator5));
      assertTrue(locator5.equals(locator5));
      assertFalse(locator5.equals(locator4));

      String url = locator.getLocatorURI();
      String origUrl = locator.getOriginalURI();

//      assertEquals("http://127.0.0.1:1234/services/uri:Test", url);
      assertEquals("http://localhost:1234/services/uri:Test", url);
      assertEquals("http://localhost:1234/services/uri:Test", origUrl);

      locator = new InvokerLocator("http://localhost/services/uri:Test");

      url = locator.getLocatorURI();
      origUrl = locator.getOriginalURI();

//      assertEquals("http://127.0.0.1/services/uri:Test", url);
      assertEquals("http://localhost/services/uri:Test", url);
      assertEquals("http://localhost/services/uri:Test", origUrl);

      locator = new InvokerLocator("socket://localhost:1234");

      url = locator.getLocatorURI();
      origUrl = locator.getOriginalURI();

//      assertEquals("socket://127.0.0.1:1234/", url);
      assertEquals("socket://localhost:1234/", url);
      assertEquals("socket://localhost:1234", origUrl);

      locator = new InvokerLocator("socket://localhost:1234/some/path");

      url = locator.getLocatorURI();
      origUrl = locator.getOriginalURI();

//      assertEquals("socket://127.0.0.1:1234/some/path", url);
      assertEquals("socket://localhost:1234/some/path", url);
      assertEquals("socket://localhost:1234/some/path", origUrl);

      locator = new InvokerLocator("socket://myhost:1234");

      url = locator.getLocatorURI();
      origUrl = locator.getOriginalURI();

      assertEquals("socket://myhost:1234/", url);
      assertEquals("socket://myhost:1234", origUrl);

      locator = new InvokerLocator("socket://myhost");

      url = locator.getLocatorURI();
      origUrl = locator.getOriginalURI();

      assertEquals("socket://myhost/", url);
      assertEquals("socket://myhost", origUrl);

      locator = new InvokerLocator("socket://myhost");

      url = locator.getLocatorURI();
      origUrl = locator.getOriginalURI();

      assertEquals("socket://myhost/", url);
      assertEquals("socket://myhost", origUrl);

   }
   
   /**
    * testURILocatorConfig() tests the new version of InvokerLocator parsing.
    */
   public void testURILocatorConfig() throws Exception
   {
      InvokerLocator.setUseLegacyParsing(false);
      System.setProperty(InvokerLocator.LEGACY_PARSING, "false");
      InvokerLocator locator = new InvokerLocator("http://localhost:1234/services/uri:Test");
      InvokerLocator locator2 = new InvokerLocator("http://localhost:1234");
      InvokerLocator locator3 = new InvokerLocator("http://127.0.0.1:1234");
      InvokerLocator locator4 = new InvokerLocator("http://1.2.3.4/aaa");
      InvokerLocator locator5 = new InvokerLocator("http://1.2.3.4/abB");
      
      // Test that path starts with '/', in accordance with
      // RFC 2396 "Uniform Resource Identifiers (URI): Generic Syntax" 
      assertEquals("/services/uri:Test", locator.getPath());
      assertEquals("", locator2.getPath());
      assertEquals("/aaa", locator4.getPath());
      assertEquals("/abB", locator5.getPath());

      assertFalse(locator.equals(null));
      assertFalse(locator.equals(locator2));
      assertFalse(locator.isSameEndpoint(null));
      assertTrue(locator.isSameEndpoint(locator2));
      assertFalse(locator.equals(locator3));
      assertFalse(locator.isSameEndpoint(locator3));

      assertTrue(locator4.equals(locator4));
      assertFalse(locator4.equals(locator5));
      assertTrue(locator5.equals(locator5));
      assertFalse(locator5.equals(locator4));

      String url = locator.getLocatorURI();
      String origUrl = locator.getOriginalURI();

//      assertEquals("http://127.0.0.1:1234/services/uri:Test", url);
      assertEquals("http://localhost:1234/services/uri:Test", url);
      assertEquals("http://localhost:1234/services/uri:Test", origUrl);

      locator = new InvokerLocator("http://localhost/services/uri:Test");

      url = locator.getLocatorURI();
      origUrl = locator.getOriginalURI();

//      assertEquals("http://127.0.0.1/services/uri:Test", url);
      assertEquals("http://localhost/services/uri:Test", url);
      assertEquals("http://localhost/services/uri:Test", origUrl);

      locator = new InvokerLocator("socket://localhost:1234");

      url = locator.getLocatorURI();
      origUrl = locator.getOriginalURI();

//      assertEquals("socket://127.0.0.1:1234/", url);
      assertEquals("socket://localhost:1234/", url);
      assertEquals("socket://localhost:1234", origUrl);

      locator = new InvokerLocator("socket://localhost:1234/some/path");

      url = locator.getLocatorURI();
      origUrl = locator.getOriginalURI();

//      assertEquals("socket://127.0.0.1:1234/some/path", url);
      assertEquals("socket://localhost:1234/some/path", url);
      assertEquals("socket://localhost:1234/some/path", origUrl);

      locator = new InvokerLocator("socket://myhost:1234");

      url = locator.getLocatorURI();
      origUrl = locator.getOriginalURI();

      assertEquals("socket://myhost:1234/", url);
      assertEquals("socket://myhost:1234", origUrl);

      locator = new InvokerLocator("socket://myhost");

      url = locator.getLocatorURI();
      origUrl = locator.getOriginalURI();

      assertEquals("socket://myhost/", url);
      assertEquals("socket://myhost", origUrl);

      locator = new InvokerLocator("socket://myhost");

      url = locator.getLocatorURI();
      origUrl = locator.getOriginalURI();

      assertEquals("socket://myhost/", url);
      assertEquals("socket://myhost", origUrl);

   }
   
   public void testJBREM645() throws MalformedURLException
   {
      InvokerLocator.setUseLegacyParsing(false);
      System.setProperty(InvokerLocator.LEGACY_PARSING, "false");
      String locatorURI ="socket://succubus.starkinternational.com:4446?datatype=invocation";
      InvokerLocator locator = new InvokerLocator(locatorURI);
      int port = locator.getPort();
      assertEquals(4446, port);
   }

   public void testMultihome() throws Throwable
   {
      // Test with host == "multihome".
      String url = "socket://multihome:44/?connecthomes=a.b:55!c.d!e.f:66&homes=g.h:77!i.j";
      InvokerLocator locator = new InvokerLocator(url);
      assertEquals("multihome", locator.getHost());
      String connectHomes = locator.getConnectHomes();
      assertEquals("a.b:55!c.d:44!e.f:66", connectHomes);
      List connectHomeList = locator.getConnectHomeList();
      assertEquals(3, connectHomeList.size());
      assertEquals(new Home("a.b", 55), connectHomeList.get(0));
      assertEquals(new Home("c.d", 44), connectHomeList.get(1));
      assertEquals(new Home("e.f", 66), connectHomeList.get(2));
      String homes = locator.getHomes();
      assertEquals("g.h:77!i.j:44", homes);
      List homeList = locator.getHomeList();
      assertEquals(2, homeList.size());
      assertEquals(new Home("g.h", 77), homeList.get(0));
      assertEquals(new Home("i.j", 44), homeList.get(1));
      
      url = "socket://multihome:44/?homes=a.b:55!c.d!e.f:66";
      locator = new InvokerLocator(url);
      assertEquals("multihome", locator.getHost());
      homes = locator.getHomes();
      assertEquals("a.b:55!c.d:44!e.f:66", homes);
      homeList = locator.getHomeList();
      assertEquals(3, homeList.size());
      assertEquals(new Home("a.b", 55), homeList.get(0));
      assertEquals(new Home("c.d", 44), homeList.get(1));
      assertEquals(new Home("e.f", 66), homeList.get(2));
      connectHomes = locator.getConnectHomes();
      assertEquals("", connectHomes);
      connectHomeList = locator.getConnectHomeList();
      assertEquals(0, connectHomeList.size());
      
      // Test with host != "multihome".
      url = "socket://jboss.org:44/?connecthomes=a.b:55!c.d!e.f:66&homes=g.h:77!i.j";
      locator = new InvokerLocator(url);
      assertEquals("jboss.org", locator.getHost());
      assertEquals(44, locator.getPort());
      assertEquals("g.h:77!i.j:44", locator.getHomes());
      assertEquals(2, locator.getHomeList().size());
      assertTrue(locator.getHomeList().contains(new Home("g.h", 77)));
      assertTrue(locator.getHomeList().contains(new Home("i.j", 44)));
      assertEquals("jboss.org:44", locator.getConnectHomes());
      assertEquals(1, locator.getConnectHomeList().size());
      assertTrue(locator.getConnectHomeList().contains(new Home("jboss.org", 44)));
      
      url = "socket://jboss.org:44/?homes=a.b:55!c.d!e.f:66";
      locator = new InvokerLocator(url);
      assertEquals("jboss.org", locator.getHost());
      assertEquals(44, locator.getPort());
      assertEquals("a.b:55!c.d:44!e.f:66", locator.getHomes());
      homeList = locator.getHomeList();
      assertEquals(3, homeList.size());
      assertTrue(homeList.contains(new Home("a.b", 55)));
      assertTrue(homeList.contains(new Home("c.d", 44)));
      assertTrue(homeList.contains(new Home("e.f", 66)));
      
      connectHomes = locator.getConnectHomes();
      assertEquals("jboss.org:44", connectHomes);
      connectHomeList = locator.getConnectHomeList();
      assertEquals(1, connectHomeList.size());
      assertEquals(new Home("jboss.org", 44), connectHomeList.get(0));
      
      // Test InvokerLocator.isMultihome().
      locator = new InvokerLocator("socket://jboss.org:44");
      assertFalse(locator.isMultihome());
      locator = new InvokerLocator("socket://jboss.org:44/?homes=a.b:55!c.d!e.f:66");
      assertFalse(locator.isMultihome());
      locator = new InvokerLocator("socket://jboss.org:44/?connecthomes=a.b:55!c.d!e.f:66");
      assertFalse(locator.isMultihome());
      locator = new InvokerLocator("socket://multihome/?homes=a.b:55!c.d!e.f:66");
      assertTrue(locator.isMultihome());
   }
   
   public void testBasicCompatibilityNoMultihome() throws Throwable
   {
      InvokerLocator l1 = new InvokerLocator("socket://a.b:8888/some/path/?y=z");
      InvokerLocator l2 = new InvokerLocator("socket://c.d:8888/some/path/?y=z");
      InvokerLocator l3 = new InvokerLocator("socket://a.b:9999/some/path/?y=z");
      InvokerLocator l4 = new InvokerLocator("socket://a.b:8888/other/path/?y=z");
      InvokerLocator l5 = new InvokerLocator("socket://a.b:8888/some/path/?a=b");
      
      assertTrue(l1.isCompatibleWith(l1));
      assertFalse(l1.isCompatibleWith(l2));
      assertFalse(l1.isCompatibleWith(l3));
      assertFalse(l1.isCompatibleWith(l4));
      assertFalse(l1.isCompatibleWith(l5)); 
      
      assertTrue(l1.isCompatibleWith(l1));
      assertFalse(l2.isCompatibleWith(l1));
      assertFalse(l3.isCompatibleWith(l1));
      assertFalse(l4.isCompatibleWith(l1));
      assertFalse(l5.isCompatibleWith(l1));
   }
   
   public void testBasicCompatibilityWithMultihome() throws Throwable
   {
      InvokerLocator l1 = new InvokerLocator("socket://multihome:8888/some/path/?y=z&homes=a.b!c.d");
      InvokerLocator l2 = new InvokerLocator("socket://multihome:9999/some/path/?y=z&homes=a.b!c.d");
      InvokerLocator l3 = new InvokerLocator("socket://multihome:8888/other/path/?y=z&homes=a.b!c.d");
      InvokerLocator l4 = new InvokerLocator("socket://multihome:8888/some/path/?a=b&homes=a.b!c.d");
      
      assertTrue(l1.isCompatibleWith(l1));
      assertFalse(l1.isCompatibleWith(l2));
      assertFalse(l1.isCompatibleWith(l3));
      assertFalse(l1.isCompatibleWith(l4)); 
      
      assertFalse(l2.isCompatibleWith(l1));
      assertTrue(l2.isCompatibleWith(l2));
      assertFalse(l2.isCompatibleWith(l3));
      assertFalse(l2.isCompatibleWith(l4)); 
      
      assertFalse(l3.isCompatibleWith(l1));
      assertFalse(l3.isCompatibleWith(l2));
      assertTrue(l3.isCompatibleWith(l3));
      assertFalse(l3.isCompatibleWith(l4));
      
      assertFalse(l4.isCompatibleWith(l1));
      assertFalse(l4.isCompatibleWith(l2));
      assertFalse(l4.isCompatibleWith(l3));
      assertTrue(l4.isCompatibleWith(l4));
   }
   
   public void testMultihomeCompatibility() throws Throwable
   {
      InvokerLocator l1 = new InvokerLocator("socket://a.b:11/some/path/?y=z");
      InvokerLocator l2 = new InvokerLocator("socket://c.d:22/some/path/?y=z");
      InvokerLocator l3 = new InvokerLocator("socket://multihome:11/some/path/?y=z&homes=a.b!c.d");
      InvokerLocator l4 = new InvokerLocator("socket://multihome:11/some/path/?y=z&homes=a.b!c.d!e.f");
      InvokerLocator l5 = new InvokerLocator("socket://multihome:11/some/path/?y=z&homes=a.b:22!e.f!g.h");
      InvokerLocator l6 = new InvokerLocator("socket://multihome:11/some/path/?y=z&connecthomes=a.b&homes=a.b:22!c.d:22");
      
      assertTrue(l1.isCompatibleWith(l1));
      assertFalse(l1.isCompatibleWith(l2));
      assertTrue(l1.isCompatibleWith(l3));
      assertTrue(l1.isCompatibleWith(l4));
      assertFalse(l1.isCompatibleWith(l5));
      assertTrue(l1.isCompatibleWith(l6));
      
      assertFalse(l2.isCompatibleWith(l1));
      assertTrue(l2.isCompatibleWith(l2));
      assertFalse(l2.isCompatibleWith(l3));
      assertFalse(l2.isCompatibleWith(l4));
      assertFalse(l2.isCompatibleWith(l5));
      assertTrue(l2.isCompatibleWith(l6));
      
      assertTrue(l3.isCompatibleWith(l1));
      assertFalse(l3.isCompatibleWith(l2));
      assertTrue(l3.isCompatibleWith(l3));
      assertTrue(l3.isCompatibleWith(l4));
      assertFalse(l3.isCompatibleWith(l5));
      assertTrue(l3.isCompatibleWith(l6));
      
      assertTrue(l4.isCompatibleWith(l1));
      assertFalse(l4.isCompatibleWith(l2));
      assertTrue(l4.isCompatibleWith(l3));
      assertTrue(l4.isCompatibleWith(l4));
      assertTrue(l4.isCompatibleWith(l5));
      assertTrue(l4.isCompatibleWith(l6));
      
      assertFalse(l5.isCompatibleWith(l1));
      assertFalse(l5.isCompatibleWith(l2));
      assertFalse(l5.isCompatibleWith(l3));
      assertTrue(l5.isCompatibleWith(l4));
      assertTrue(l5.isCompatibleWith(l5));
      assertTrue(l5.isCompatibleWith(l6));
      
      assertTrue(l6.isCompatibleWith(l1));
      assertFalse(l6.isCompatibleWith(l2));
      assertTrue(l6.isCompatibleWith(l3));
      assertTrue(l6.isCompatibleWith(l4));
      assertFalse(l6.isCompatibleWith(l5));
      assertTrue(l6.isCompatibleWith(l6));
   }
   
   
   public void testMultihomeDefaultPorts() throws Throwable
   {
      InvokerLocator l = new InvokerLocator("socket://multihome/?homes=a.b!c.d:12");
      assertEquals("a.b:-1!c.d:12", l.getHomes());
      l = new InvokerLocator("socket://multihome/?homes=a.b!c.d:12&" + InvokerLocator.DEFAULT_PORT + "=13");
      assertEquals("a.b:13!c.d:12", l.getHomes());
      l = new InvokerLocator("socket://multihome/?homes=a.b!c.d:12&" + InvokerLocator.DEFAULT_CONNECT_PORT + "=14");
      assertEquals("a.b:-1!c.d:12", l.getHomes());
      l = new InvokerLocator("socket://multihome/?homes=a.b!c.d:12&" + InvokerLocator.DEFAULT_PORT + "=13&" + InvokerLocator.DEFAULT_CONNECT_PORT + "=14");
      assertEquals("a.b:13!c.d:12", l.getHomes());
      
      l = new InvokerLocator("socket://multihome:11/?homes=a.b!c.d:12");
      assertEquals("a.b:11!c.d:12", l.getHomes());
      l = new InvokerLocator("socket://multihome:11/?homes=a.b!c.d:12&" + InvokerLocator.DEFAULT_PORT + "=13");
      assertEquals("a.b:13!c.d:12", l.getHomes());
      l = new InvokerLocator("socket://multihome:11/?homes=a.b!c.d:12&" + InvokerLocator.DEFAULT_CONNECT_PORT + "=14");
      assertEquals("a.b:11!c.d:12", l.getHomes());
      l = new InvokerLocator("socket://multihome:11/?homes=a.b!c.d:12&" + InvokerLocator.DEFAULT_PORT + "=13&" + InvokerLocator.DEFAULT_CONNECT_PORT + "=14");
      assertEquals("a.b:13!c.d:12", l.getHomes());
      
      l = new InvokerLocator("socket://multihome/?connecthomes=a.b!c.d:12");
      assertEquals("a.b:-1!c.d:12", l.getConnectHomes());
      l = new InvokerLocator("socket://multihome/?connecthomes=a.b!c.d:12&" + InvokerLocator.DEFAULT_PORT + "=13");
      assertEquals("a.b:13!c.d:12", l.getConnectHomes());
      l = new InvokerLocator("socket://multihome/?connecthomes=a.b!c.d:12&" + InvokerLocator.DEFAULT_CONNECT_PORT + "=14");
      assertEquals("a.b:14!c.d:12", l.getConnectHomes());
      l = new InvokerLocator("socket://multihome/?connecthomes=a.b!c.d:12&" + InvokerLocator.DEFAULT_PORT + "=13&" + InvokerLocator.DEFAULT_CONNECT_PORT + "=14");
      assertEquals("a.b:14!c.d:12", l.getConnectHomes());
      
      l = new InvokerLocator("socket://multihome:11/?connecthomes=a.b!c.d:12");
      assertEquals("a.b:11!c.d:12", l.getConnectHomes());
      l = new InvokerLocator("socket://multihome:11/?connecthomes=a.b!c.d:12&" + InvokerLocator.DEFAULT_PORT + "=13");
      assertEquals("a.b:13!c.d:12", l.getConnectHomes());
      l = new InvokerLocator("socket://multihome:11/?connecthomes=a.b!c.d:12&" + InvokerLocator.DEFAULT_CONNECT_PORT + "=14");
      assertEquals("a.b:14!c.d:12", l.getConnectHomes());
      l = new InvokerLocator("socket://multihome:11/?connecthomes=a.b!c.d:12&" + InvokerLocator.DEFAULT_PORT + "=13&" + InvokerLocator.DEFAULT_CONNECT_PORT + "=14");
      assertEquals("a.b:14!c.d:12", l.getConnectHomes());
   }
   
   
   /**
    * For JBREM-936.
    */
   public void testNullHost() throws Exception
   {
      InvokerLocator locator = new InvokerLocator("socket://:7777");
      boolean byHost = true;
      String host = null;

      try
      {             
         String s = System.getProperty(InvokerLocator.BIND_BY_HOST, "True");
         byHost = Boolean.valueOf(s).booleanValue();
      }
      catch(Exception e)
      {
      }
      if(byHost)
      {
         host = InetAddress.getLocalHost().getHostName();
      }
      else
      {
         host = InetAddress.getLocalHost().getHostAddress();
      }

      assertEquals(host, locator.getHost());
   }
   
   /**
    * For JBREM-1110.
    */
   public void testEmptyParameters() throws Exception
   {
      InvokerLocator locator = new InvokerLocator("socket://:7777");
      assertNotNull(locator.getParameters());
   }
}