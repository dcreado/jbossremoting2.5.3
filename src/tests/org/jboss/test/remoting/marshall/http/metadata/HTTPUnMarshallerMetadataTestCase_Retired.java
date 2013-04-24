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

package org.jboss.test.remoting.marshall.http.metadata;

import junit.framework.TestCase;
import org.apache.log4j.Level;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Test case that uses the HTTPInvoker client to call on two different public SOAP services (one based
 * on Axis and the other based on .NET implementations).
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class HTTPUnMarshallerMetadataTestCase_Retired extends TestCase
{
   private Client client;

   public void init(String httpTargetURL, HTTPUnMarshallerMock unmarshaller)
   {
      try
      {
         InvokerLocator locator = new InvokerLocator(httpTargetURL);
         client = new Client(locator);
         client.connect();
         client.setUnMarshaller(unmarshaller);
         client.connect();
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }

   public String makeInvocationCall(String httpTargetURL, String payload, Map metadata, HTTPUnMarshallerMock unmarshaller) throws Throwable
   {
      init(httpTargetURL, unmarshaller);

      Object obj = client.invoke(payload, metadata);

      System.out.println("invoke returned" + obj);

      return (String) obj;
   }

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

      Properties headerProps = new Properties();
      headerProps.put("SOAPAction", "");
      headerProps.put("Content-type", "text/xml; charset=UTF-8");

      metadata.put("HEADER", headerProps);


      HTTPUnMarshallerMetadataTestCase_Retired client = new HTTPUnMarshallerMetadataTestCase_Retired();

      HTTPUnMarshallerMock unmarshaller = new HTTPUnMarshallerMock();

      String result = client.makeInvocationCall(testURL, xml, metadata, unmarshaller);

      // expect to get the following header
      assertNotNull(unmarshaller.getMetadata());
      List header = (List) unmarshaller.getMetadata().get("Status");
      assertEquals("200", header.get(0));

      // don't need to comapre full string. (as actual temp value will change each time run)
      assertEquals(getExpectedWeatherResult().substring(0, 380), result.substring(0, 380));

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


}