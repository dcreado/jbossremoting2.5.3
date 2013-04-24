/***************************************
 *                                     *
 *  JBoss: The OpenSource J2EE WebOS   *
 *                                     *
 *  Distributable under LGPL license.  *
 *  See terms of license at gnu.org.   *
 *                                     *
 ***************************************/
package org.jboss.test.remoting.transport.http.proxy;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is for testing http client invocations through
 * public proxy server.  Is not a great tests as there is no way
 * to really prove went through the proxy.  Best test can do
 * is indicate problem with config.  Also, test may fail just due
 * to not being able to use proxy server (as is a public proxy server
 * in China and have no control over it.  for more public proxy server
 * listings, can see http://www.publicproxyservers.com/page1.html).
 * Also, no gurantee the content on testUrl will never be changed.
 *
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class HTTPInvokerProxyTestCase_Retired extends TestCase
{
   private Client client;
//   private String testUrl = "http://www.gnu.org/licenses/gpl.html";
   private String testUrl = "http://www.ietf.org/rfc/rfc1766.txt?number=1766";

   public void setUp()
   {
      init(testUrl);
   }

   public void init(String httpTargetURL)
   {
      try
      {
         InvokerLocator locator = new InvokerLocator(httpTargetURL);
         Map config = new HashMap();
         client = new Client(locator, config);
         client.connect();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   public String makeInvocationCall(String payload, Map metadata) throws Throwable
   {
      Object obj = client.invoke(payload, metadata);

      System.out.println("invoke returned" + obj);

      return (String) obj;
   }

   public void testHTTPProxyInvocationViaSystemConfig() throws Throwable
   {
      Map metadata = new HashMap();
      metadata.put(Client.RAW, Boolean.TRUE);
      metadata.put("TYPE", "GET");


      // proxy info
      System.setProperty("proxySet", "true");
      System.setProperty("http.proxyHost", "84.19.177.62");
      System.setProperty("http.proxyPort", "8080");

      String result = makeInvocationCall(null, metadata);

      System.out.println("invoke returned" + result);

      assertEquals(getExpectedGETResult().substring(0, 50), result.substring(0, 50));

   }

   public void testHTTPProxyInvocationViaConfig() throws Throwable
   {
      // need to make sure is running jdk 1.5 or higher.
      // otherwise, no point in running this test method as will
      // not work with jdk 1.4.
      boolean isJDK15 = false;

      try
      {
         Class proxyClass = Class.forName("java.net.Proxy");
         isJDK15 = true;
      }
      catch (ClassNotFoundException e)
      {
         System.out.println("Not running jdk 1.5 or higher, so will bypass testHTTPProxyInvocationViaConfig test.");
      }

      if(isJDK15)
      {
         Map metadata = new HashMap();
         metadata.put(Client.RAW, Boolean.TRUE);
         metadata.put("TYPE", "GET");


         // proxy info
         metadata.put("http.proxyHost", "84.19.177.62");
         metadata.put("http.proxyPort", "8080");

         String result = makeInvocationCall(null, metadata);

         System.out.println("invoke returned" + result);

         assertEquals(getExpectedGETResult().substring(0, 50), result.substring(0, 50));
      }
   }

   private String getExpectedGETResult()
   {
      return "Network Working Group                                      H. AlvestrandRequest for Comments: 1766  ";
   }

/*
   private String getExpectedGETResult()
   {
      return "<!DOCTYPE html PUBLIC \"-//IETF//DTD HTML 2.0//EN\"><HTML><HEAD><TITLE>GNU General Public License - " +
             "GNU Project - Free Software Foundation (FSF)</TITLE><LINK REV=\"made\" " +
             "HREF=\"mailto:webmasters@www.gnu.org\"><link rel=\"stylesheet\" type=\"text/css\" href=\"/gnu.css\" />" +
             "</HEAD><BODY BGCOLOR=\"#FFFFFF\" TEXT=\"#000000\" LINK=\"#1F00FF\" ALINK=\"#FF0000\" VLINK=\"#9900DD\">" +
             "<H1>GNU General Public License</H1><A HREF=\"/graphics/philosophicalgnu.html\"><IMG SRC=\"/graphics/philosophical-gnu-sm.jpg\"   " +
             "ALT=\" [image of a Philosophical GNU] \"   WIDTH=\"160\" HEIGHT=\"200\"></A><!-- Please keep this list alphabetical -->" +
             "<!-- tower, gpl.ja.html is Japanese translation of THIS PAGE, --><!-- NOT translation of GPL itself(gpl.ja.html contains the original --> " +
             "<!-- English version). So please do not remove the following. --><!-- Thanks -mhatta -->" +
             "<!-- The same for the Czech page. The entire text of GPL is not --><!-- translated on this page. Thanks Sisao -->[   " +
             "<A HREF=\"/licenses/gpl.cs.html\">Czech</A>| <A HREF=\"/licenses/gpl.html\">English</A>| <A HREF=\"/licenses/gpl.ja.html\">Japanese</A>]" +
             "<!-- It is best to not enumerate the translations here in a menu bar, --><!-- It is best to have the users follow this link, so they have the FSF' -->" +
             "<!-- explanation about translations being unofficial, etc. --><P><UL>  <LI>" +
             "<A HREF=\"/licenses/gpl-violation.html\"><EM>What to do if you see a       possible GPL violation</EM></A>  <LI>" +
             "<A HREF=\"/licenses/translations.html\"><EM>Translations       of the GPL</EM></A>  <LI><A HREF=\"/licenses/gpl-faq.html\"" +
             "><EM>GPL Frequently Asked Questions</EM></A>  <LI>The GNU General Public License (GPL)       " +
             "<A HREF=\"/licenses/gpl.txt\">in plain text format</A>  <LI>The GNU General Public License (GPL)       " +
             "<A HREF=\"/licenses/gpl.texi\">in Texinfo format</A>  <LI>The GNU General Public License (GPL)       " +
             "<A HREF=\"/licenses/gpl-2.0.tex\">in LaTeX format</A>  <li>The GNU General Public License (GPL)       " +
             "<a href=\"/licenses/gpl.dbk\">as an appendix in DocBook format</a></UL><P>       <HR><P><H2>Table of Contents</H2><UL>  " +
             "<LI><A NAME=\"TOC1\" HREF=\"gpl.html#SEC1\">GNU GENERAL PUBLIC LICENSE</A><UL><LI><A NAME=\"TOC2\" HREF=\"gpl.html#SEC2\">Preamble</A>";
   }
*/

}