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

package org.jboss.test.remoting.transport.http;

import junit.framework.TestCase;
import org.apache.log4j.Level;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Test case that uses the HTTPInvoker client to call on two different public SOAP services (one based
 * on Axis and the other based on .NET implementations).
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class HTTPInvokerClientTestCase_deleted extends TestCase
{
   private Client client;

   public void init(String httpTargetURL)
   {
      try
      {
         InvokerLocator locator = new InvokerLocator(httpTargetURL);
         Map config = new HashMap();
         config.put(Client.ENABLE_LEASE, "false");
         client = new Client(locator, config);
         client.connect();
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }

   public String makeInvocationCall(String httpTargetURL, String payload, Map metadata) throws Throwable
   {
      init(httpTargetURL);
      Object obj = client.invoke(payload, metadata);

      System.out.println("invoke returned" + obj);

      return (String) obj;
   }

   /* *** commented out because external site no longer available ***
   public void testWeatherHTTPInvocation() throws Throwable
   {

      org.apache.log4j.BasicConfigurator.configure();
      org.apache.log4j.Category.getRoot().setLevel(Level.INFO);
      org.apache.log4j.Category.getInstance("org.jgroups").setLevel(Level.INFO);
      org.apache.log4j.Category.getInstance("org.jboss.remoting").setLevel(Level.DEBUG);
      org.apache.log4j.Category.getInstance("test").setLevel(Level.DEBUG);


      String testURL = "http://services.xmethods.net:80/soap/servlet/rpcrouter";

      String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                   "<soap:Envelope xmlns:mrns0=\"urn:xmethods-Temperature\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                   "   <soap:Body soap:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
                   "      <mrns0:getTemp>\n" +
                   "         <zipcode xsi:type=\"xs:string\">30106</zipcode>\n" +
                   "      </mrns0:getTemp>\n" +
                   "   </soap:Body>\n" +
                   "</soap:Envelope>";

      Map metadata = new HashMap();
      metadata.put(Client.RAW, Boolean.TRUE);
      metadata.put("TYPE", "POST");

      // proxy info
      //metadata.put("http.proxyHost", "ginger");
      //metadata.put("http.proxyPort", "80");
      //metadata.put("http.proxy.username", "tom");
      //metadata.put("http.proxy.password", "foobar");

      Properties headerProps = new Properties();
      headerProps.put("SOAPAction", "");
      headerProps.put("Content-type", "text/xml; charset=UTF-8");

      metadata.put("HEADER", headerProps);


      HTTPInvokerClientTestCase client = new HTTPInvokerClientTestCase();

      String result = client.makeInvocationCall(testURL, xml, metadata);
      // don't need to comapre full string. (as actual temp value will change each time run)
      assertEquals(getExpectedWeatherResult().substring(0, 380), result.substring(0, 380));

   }
   */

   public void testCitiesByCountryHTTPInvocation() throws Throwable
   {

      org.apache.log4j.BasicConfigurator.configure();
      org.apache.log4j.Category.getRoot().setLevel(Level.INFO);
      org.apache.log4j.Category.getInstance("org.jgroups").setLevel(Level.INFO);
      org.apache.log4j.Category.getInstance("org.jboss.remoting").setLevel(Level.DEBUG);
      org.apache.log4j.Category.getInstance("test").setLevel(Level.DEBUG);


      String testURL = "http://www.webserviceX.NET/globalweather.asmx?op=GetCitiesByCountry";

      String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                   "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                   "  <soap:Body>\n" +
                   "    <GetCitiesByCountry xmlns=\"http://www.webserviceX.NET\">\n" +
                   "      <CountryName>Germany</CountryName>\n" +
                   "    </GetCitiesByCountry>\n" +
                   "  </soap:Body>\n" +
                   "</soap:Envelope>";

      Map metadata = new HashMap();
      metadata.put(Client.RAW, Boolean.TRUE);
      metadata.put("TYPE", "POST");

      // proxy info
      //metadata.put("http.proxyHost", "ginger");
      //metadata.put("http.proxyPort", "80");
      //metadata.put("http.proxy.username", "tom");
      //metadata.put("http.proxy.password", "foobar");

      Properties headerProps = new Properties();
      headerProps.put("SOAPAction", "http://www.webserviceX.NET/GetCitiesByCountry");
      headerProps.put("Content-type", "text/xml; charset=UTF-8");

      metadata.put("HEADER", headerProps);


      HTTPInvokerClientTestCase_deleted client = new HTTPInvokerClientTestCase_deleted();

      String result = client.makeInvocationCall(testURL, xml, metadata);
      // 30 characters will be good enough.
      assertEquals(getExpectedCityResult().substring(0, 200), result.substring(0, 200));

   }


//   public void testGETHTTPInvocation() throws Throwable
//   {
//
//      org.apache.log4j.BasicConfigurator.configure();
//      org.apache.log4j.Category.getRoot().setLevel(Level.INFO);
//      org.apache.log4j.Category.getInstance("org.jgroups").setLevel(Level.INFO);
//      org.apache.log4j.Category.getInstance("org.jboss.remoting").setLevel(Level.DEBUG);
//      org.apache.log4j.Category.getInstance("test").setLevel(Level.DEBUG);
//
//
//      String testURL = "http://www.gnu.org/licenses/gpl.html";
//
//      Map metadata = new HashMap();
//      metadata.put(Client.RAW, Boolean.TRUE);
//      metadata.put("TYPE", "GET");
//
//      HTTPInvokerClientTestCase client = new HTTPInvokerClientTestCase();
//
//      String result = client.makeInvocationCall(testURL, null, metadata);
//      // 30 characters will be good enough.
//      assertEquals(getExpectedGETResult().substring(0, 100), result.substring(0, 100));
//
//   }

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


   private String getExpectedWeatherResult()
   {
      return "<?xml version='1.0' encoding='UTF-8'?>" +
             "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">" +
             "<SOAP-ENV:Body>" +
             "<ns1:getTempResponse xmlns:ns1=\"urn:xmethods-Temperature\" SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
             "<return xsi:type=\"xsd:float\">60.0</return>" +
             "</ns1:getTempResponse>" +
             "\n" +
             "</SOAP-ENV:Body>\n" +
             "</SOAP-ENV:Envelope>";
   }

   private String getExpectedCityResult()
   {
      return "<?xml version=\"1.0\" encoding=\"utf-8\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
             "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">" +
             "<soap:Body><GetCitiesByCountryResponse xmlns=\"http://www.webserviceX.NET\"><GetCitiesByCountryResult>&lt;NewDataSet&gt;  " +
             "&lt;Table&gt;    &lt;Country&gt;Germany&lt;/Country&gt;    &lt;City&gt;Berlin-Schoenefeld&lt;/City&gt;  &lt;/Table&gt;  &lt;Table&gt;    " +
             "&lt;Country&gt;Germany&lt;/Country&gt;    &lt;City&gt;Dresden-Klotzsche&lt;/City&gt;  &lt;/Table&gt;  &lt;Table&gt;   " +
             " &lt;Country&gt;Germany&lt;/Country&gt;    &lt;City&gt;Erfurt-Bindersleben&lt;/City&gt;  &lt;/Table&gt;  &lt;Table&gt;   " +
             " &lt;Country&gt;Germany&lt;/Country&gt;    &lt;City&gt;Frankfurt / M-Flughafen&lt;/City&gt;  &lt;/Table&gt;  &lt;Table&gt;    " +
             "&lt;Country&gt;Germany&lt;/Country&gt;    &lt;City&gt;Muenster / Osnabrueck&lt;/City&gt;  &lt;/Table&gt;  &lt;Table&gt;    " +
             "&lt;Country&gt;Germany&lt;/Country&gt;    &lt;City&gt;Hamburg-Fuhlsbuettel&lt;/City&gt;  &lt;/Table&gt;  &lt;Table&gt;   " +
             " &lt;Country&gt;Germany&lt;/Country&gt;    &lt;City&gt;Berlin-Tempelhof&lt;/City&gt;  &lt;/Table&gt;  &lt;Table&gt;    " +
             "&lt;Country&gt;Germany&lt;/Country&gt;    &lt;City&gt;Koeln / Bonn&lt;/City&gt;  &lt;/Table&gt;  &lt;Table&gt;    " +
             "&lt;Country&gt;Germany&lt;/Country&gt;    &lt;City&gt;Duesseldorf&lt;/City&gt;  &lt;/Table&gt;  &lt;Table&gt;   " +
             " &lt;Country&gt;Germany&lt;/Country&gt;    &lt;City&gt;Munich / Riem&lt;/City&gt;  &lt;/Table&gt;  &lt;Table&gt;   " +
             " &lt;Country&gt;Germany&lt;/Country&gt;    &lt;City&gt;Nuernberg&lt;/City&gt;  &lt;/Table&gt;  &lt;Table&gt;   " +
             " &lt;Country&gt;Germany&lt;/Country&gt;    &lt;City&gt;Leipzig-Schkeuditz&lt;/City&gt;  &lt;/Table&gt;  &lt;Table&gt;  " +
             "  &lt;Country&gt;Germany&lt;/Country&gt;    &lt;City&gt;Saarbruecken / Ensheim&lt;/City&gt;  &lt;/Table&gt;  &lt;Table&gt;    " +
             "&lt;Country&gt;Germany&lt;/Country&gt;    &lt;City&gt;Stuttgart-Echterdingen&lt;/City&gt;  &lt;/Table&gt;  &lt;Table&gt;  " +
             "  &lt;Country&gt;Germany&lt;/Country&gt;    &lt;City&gt;Berlin-Tegel&lt;/City&gt;  &lt;/Table&gt;  &lt;Table&gt;    " +
             "&lt;Country&gt;Germany&lt;/Country&gt;    &lt;City&gt;Hannover&lt;/City&gt;  &lt;/Table&gt;  &lt;Table&gt;    " +
             "&lt;Country&gt;Germany&lt;/Country&gt;    &lt;City&gt;Bremen&lt;/City&gt;  &lt;/Table&gt;  &lt;Table&gt;  " +
             "  &lt;Country&gt;Germany&lt;/Country&gt;    &lt;City&gt;Hahn&lt;/City&gt;  &lt;/Table&gt;  &lt;Table&gt;" +
             "    &lt;Country&gt;Germany&lt;/Country&gt;    &lt;City&gt;Baden Wurttemberg, Neuostheim&lt;/";
   }

//   public static void main(String[] args)
//   {
//      HTTPInvokerClientTest test = new HTTPInvokerClientTest();
//      test.testHTTPInvocation();
//   }

}