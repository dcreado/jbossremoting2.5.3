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

package org.jboss.remoting.transport.servlet.web;

import org.jboss.logging.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.InvokerRegistry;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.transport.servlet.ServletServerInvokerMBean;
import org.jboss.remoting.util.SecurityUtility;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * The servlet that receives the inital http request for the ServletServerInvoker.
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class ServerInvokerServlet extends HttpServlet
{
   private static Logger log = Logger.getLogger(ServerInvokerServlet.class);
   private ServletServerInvokerMBean servletInvoker;
   private static final long serialVersionUID = 8796224225710165263L;

   /**
    * Initializes the servlet.
    */
   public void init(ServletConfig config) throws ServletException
   {
      super.init(config);

      // first see if the invoker is specified by its URL; if not, then see if the invoker was specified by name
      servletInvoker = getInvokerFromInvokerUrl(config);

      if (servletInvoker == null)
      {
         servletInvoker = getInvokerFromInvokerName(config);

         if (servletInvoker == null)
         {
            throw new ServletException("Could not find init parameter for 'locatorUrl' or 'locatorName' - one of which must be supplied for ServerInvokerServlet to function.");
         }
         else
         {
            log.debug("Got ServletServerInvoker from InvokerName: " + config.getInitParameter("invokerName"));
         }
      }
      else
      {
         log.debug("Got ServletServerInvoker from InvokerLocator: " + config.getInitParameter("locatorUrl"));
      }
   }

   /**
    * Destroys the servlet.
    */
   public void destroy()
   {

   }

   /**
    * Read a MarshalledInvocation and dispatch it to the target JMX object
    * invoke(Invocation) object.
    *
    * @param request  servlet request
    * @param response servlet response
    */
   protected void processRequest(HttpServletRequest request, HttpServletResponse response)
         throws ServletException, IOException
   {
      boolean trace = log.isTraceEnabled();
      if (trace)
      {
         log.trace("processRequest, ContentLength: " + request.getContentLength());
         log.trace("processRequest, ContentType: " + request.getContentType());
      }

      int bufferSize = 1024;
      byte[] byteBuffer = new byte[bufferSize];
      ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();

      int pointer = 0;
      int contentLength = request.getContentLength();
      ServletInputStream inputStream = request.getInputStream();
      int amtRead = inputStream.read(byteBuffer);

      while (amtRead > 0)
      {
         byteOutputStream.write(byteBuffer, pointer, amtRead);
         //pointer+=amtRead;
         if (amtRead < bufferSize && byteOutputStream.size() >= contentLength)
         {
            //done reading, so process
            break;
         }
         amtRead = inputStream.read(byteBuffer);
      }
      byteOutputStream.flush();
      byte[] totalByteArray = byteOutputStream.toByteArray();
      byte[] out = processRequest(servletInvoker, request, totalByteArray, response);
      ServletOutputStream outStream = response.getOutputStream();
      outStream.write(out);
      outStream.flush();
      outStream.close();
      //response.setContentLength(out.length);
   }

   /**
    * Handles the HTTP <code>GET</code> method.
    *
    * @param request  servlet request
    * @param response servlet response
    */
   protected void doGet(HttpServletRequest request, HttpServletResponse response)
         throws ServletException, IOException
   {
      processRequest(request, response);
   }

   /**
    * Handles the HTTP <code>POST</code> method.
    *
    * @param request  servlet request
    * @param response servlet response
    */
   protected void doPost(HttpServletRequest request, HttpServletResponse response)
         throws ServletException, IOException
   {
      processRequest(request, response);
   }

   /**
    * Returns a short description of the servlet.
    */
   public String getServletInfo()
   {
      return "Servlet front to JBossRemoting servlet server invoker.";
   }

   /**
    * Returns the servlet server invoker but only if it was specified via the
    * "invokerUrl" init parameter.
    *
    * @param config the servlet configuration
    * @return the servlet server invoker as specified by the "invokerUrl", or
    *         <code>null</code> if "invokerUrl" init parameter was not specified
    * @throws ServletException
    */
   protected ServletServerInvokerMBean getInvokerFromInvokerUrl(ServletConfig config)
         throws ServletException
   {
      String locatorUrl = config.getInitParameter("locatorUrl");
      if (locatorUrl == null)
      {
         return null;
      }
      try
      {
         InvokerLocator validatedLocator = new InvokerLocator(locatorUrl);
         locatorUrl = InvokerLocator.validateLocator(validatedLocator).getLocatorURI();
      }
      catch (MalformedURLException e)
      {
         log.warn("malformed URL: " + locatorUrl);
         return null;
      }

      ServerInvoker[] serverInvokers = InvokerRegistry.getServerInvokers();
      if (serverInvokers != null && serverInvokers.length > 0)
      {
         for (int x = 0; x < serverInvokers.length; x++)
         {
            ServerInvoker svrInvoker = serverInvokers[x];
            InvokerLocator locator = svrInvoker.getLocator();
            if (locatorUrl.equalsIgnoreCase(locator.getLocatorURI()))
            {
               return (ServletServerInvokerMBean) svrInvoker;
            }
         }

         throw new ServletException("Can not find servlet server invoker with same locator as specified (" + locatorUrl + ")");
      }

      throw new ServletException("Can not find any server invokers registered.  " +
                                 "Could be that servlet server invoker not registered or " +
                                 "has been created using different classloader.");
   }

   /**
    * Returns the servlet server invoker but only if it was specified via the
    * "invokerName" init parameter.
    *
    * @param config the servlet configuration
    * @return the servlet server invoker as specified by the "invokerName", or
    *         <code>null</code> if "invokerName" init parameter was not specified
    * @throws ServletException
    */
   protected ServletServerInvokerMBean getInvokerFromInvokerName(ServletConfig config)
         throws ServletException
   {
      ObjectName localInvokerName = null;

      String name = config.getInitParameter("invokerName");
      if (name == null)
      {
         return null;
      }

      try
      {
         localInvokerName = new ObjectName(name);
         log.debug("localInvokerName=" + localInvokerName);
      }
      catch (MalformedObjectNameException e)
      {
         throw new ServletException("Failed to build invokerName", e);
      }

      // Lookup the MBeanServer
      String mbeanServerId = config.getInitParameter("mbeanServer");
      MBeanServer mbeanServer = getMBeanServer(mbeanServerId);
      if (mbeanServer == null)
      {
         throw new ServletException("Failed to locate the MBeanServer");
      }

      return (ServletServerInvokerMBean)
            MBeanServerInvocationHandler.newProxyInstance(mbeanServer,
                                                          localInvokerName,
                                                          ServletServerInvokerMBean.class,
                                                          false);
   }

   /**
    * Returns the MBeanServer where the server invoker should be found.  This should only be
    * used if the "invokerName" init parameter is specified (where that invoker name is the
    * object name registered in the returned MBeanServer).
    *
    * @param mbeanServerId indicates which MBeanServer to use
    *
    * @return MBeanServer where the invoker is supposed to be registered
    */
   protected MBeanServer getMBeanServer(String mbeanServerId)
   {
      if (mbeanServerId == null)
      {
         mbeanServerId = "jboss";
      }

      if (mbeanServerId.equals("*platform*"))
      {
         try
         {
            MBeanServer s = getPlatformMBeanServer();
            log.debug("Using platform MBeanServer");
            return s;
         }
         catch (Exception e)
         {
            mbeanServerId = "jboss";
         }
      }
      
      Iterator i = findMBeanServer(null).iterator();
      while(i.hasNext())
      {
         MBeanServer server = (MBeanServer) i.next();
         
         if (server.getDefaultDomain() == null)
         {
            continue;
         }
         if (server.getDefaultDomain().equals(mbeanServerId))
         {
            log.debug("Using MBeanServer with defaultDomain: " + mbeanServerId);
            return server;
         }
      }
      
      return null;
   }
   
   static private ArrayList findMBeanServer(final String agentId)
   {
      if (SecurityUtility.skipAccessControl())
      {
         return MBeanServerFactory.findMBeanServer(agentId);
      }
      
      return (ArrayList)AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            return MBeanServerFactory.findMBeanServer(agentId);
         }
      });
   }
   
   static private MBeanServer getPlatformMBeanServer()
   throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
   {
      if (SecurityUtility.skipAccessControl())
      {
         Class c = null;
         try
         {
            c = Class.forName("java.lang.management.ManagementFactory");
         }
         catch (Exception e)
         {
            System.out.println("Unable to access java.lang.management.ManagementFactory: must be using jdk 1.4");
            return null;
         }
         Method m = c.getMethod("getPlatformMBeanServer", new Class[] {});
         MBeanServer s = (MBeanServer) m.invoke(null, new Object[] {});
         return s;
      }
      
      try
      {
         return (MBeanServer) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run()
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
            {
               Class c = null;
               try
               {
                  c = Class.forName("java.lang.management.ManagementFactory");
               }
               catch (Exception e)
               {
                  System.out.println("Unable to access java.lang.management.ManagementFactory: must be using jdk 1.4");
                  return null;
               }
               Method m = c.getMethod("getPlatformMBeanServer", new Class[] {});
               MBeanServer s = (MBeanServer) m.invoke(null, new Object[] {});
               return s;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
        Throwable cause = e.getCause();
        if (cause instanceof NoSuchMethodException)
           throw (NoSuchMethodException) cause;
        else if (cause instanceof IllegalAccessException)
           throw (IllegalAccessException) cause;
        else
           throw (InvocationTargetException) cause;
      }  
   }
   
   static private byte[] processRequest(final ServletServerInvokerMBean invoker,
         final HttpServletRequest request,
         final byte[] byteArray,
         final HttpServletResponse response)
   throws ServletException, IOException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return invoker.processRequest(request, byteArray, response);
      }

      try
      {
         return (byte[]) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws ServletException, IOException
            {
               return invoker.processRequest(request, byteArray, response);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         Throwable cause = e.getCause();
         if (cause instanceof ServletException)
            throw (ServletException) cause;
         else
            throw (IOException) e.getCause();
      }  
   }
}