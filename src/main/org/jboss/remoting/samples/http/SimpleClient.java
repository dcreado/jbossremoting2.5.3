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

package org.jboss.remoting.samples.http;

import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transport.http.HTTPMetadataConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple test client to make an invocation on remoting server.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class SimpleClient
{
   // Default locator values
   private static String transport = "http";
   private static String host = "localhost";
   private static int port = 5400;

   public void makeInvocation(String locatorURI) throws Throwable
   {
      // create InvokerLocator with the url type string
      // indicating the target remoting server to call upon.
      InvokerLocator locator = new InvokerLocator(locatorURI);
      System.out.println("Calling remoting server with locator uri of: " + locatorURI);

      Client remotingClient = new Client(locator);
      remotingClient.connect();

      // make invocation on remoting server and send complex data object
      // by default, the remoting http client invoker will use method type of POST,
      // which is needed when ever sending objects to the server.  So no metadata map needs
      // to be passed to the invoke() method.
      Object response = remotingClient.invoke(new ComplexObject(2, "foo", true), null);

      System.out.println("\nResponse from remoting http server when making http POST request and sending a complex data object:\n" + response);


      Map metadata = new HashMap();
      // set the metadata so remoting client knows to use http GET method type
      metadata.put("TYPE", "GET");
      // not actually sending any data to the remoting server, just want to get its response
      response = remotingClient.invoke((Object) null, metadata);

      System.out.println("\nResponse from remoting http server when making GET request:\n" + response);

      // now set type back to POST and send a plain text based request
      metadata.put("TYPE", "POST");
      response = remotingClient.invoke(WebInvocationHandler.STRING_RETURN_PARAM, metadata);

      System.out.println("\nResponse from remoting http server when making http POST request and sending a text based request:\n" + response);

      // notice are getting custom response code and message set by web invocation handler
      Integer responseCode = (Integer) metadata.get(HTTPMetadataConstants.RESPONSE_CODE);
      String responseMessage = (String) metadata.get(HTTPMetadataConstants.RESPONSE_CODE_MESSAGE);
      System.out.println("Response code from server: " + responseCode);
      System.out.println("Response message from server: " + responseMessage);

   }


   /**
    * Can pass transport and port to be used as parameters.
    * Valid transports are 'rmi' and 'socket'.
    *
    * @param args
    */
   public static void main(String[] args)
   {
      if(args != null && args.length == 3)
      {
         transport = args[0];
         host = args[1];
         port = Integer.parseInt(args[2]);
      }
      String locatorURI = transport + "://" + host + ":" + port;
      SimpleClient client = new SimpleClient();
      try
      {
         client.makeInvocation(locatorURI);
      }
      catch(Throwable e)
      {
         e.printStackTrace();
      }
   }


}