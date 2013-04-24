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
package org.jboss.remoting.callback;

import org.jboss.logging.Logger;
import org.jboss.remoting.ServerInvoker;

import java.util.Map;

/**
 * Default callback error handler if one not specified. This class will listen for exceptions that
 * occur when trying to deliver callback message to a callback listener from the server. By default,
 * after the fifth exception, it will remove that callback listener from the server invoker handler.
 * To set the number of exceptions it will allow before this happens, can set the
 * 'callbackErrorsAllowed' attribute within the invoker configuration.
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 */
public class DefaultCallbackErrorHandler implements CallbackErrorHandler
{
   // Constants ------------------------------------------------------------------------------------

   private static final Logger log = Logger.getLogger(DefaultCallbackErrorHandler.class);

   /**
    * Key for setting the number of callback exceptions will be allowed when calling on
    * org.jboss.remoting.callback.InvokerCallbackHandler.handleCallback(Callback callback)
    * before cleaning up the callback listener. This only applies to push callback. The default if
    * this property is not set is five.
    */
   public static final String CALLBACK_ERRORS_ALLOWED = "callbackErrorsAllowed";


   // defaults to 5 errors before auto clean up
   private static final int DEFAULT_NUMBER_OF_ERRORS_ALLOWED = 5;

   // Static ---------------------------------------------------------------------------------------

   // Attributes -----------------------------------------------------------------------------------

   private ServerInvoker serverInvoker;
   private ServerInvokerCallbackHandler callbackHandler;
   private int numOfErrorsAllowed;
   private int currentNumberOfErrors;
   private String handlerSubsystem;

   // Constructors ---------------------------------------------------------------------------------

   public DefaultCallbackErrorHandler()
   {
      numOfErrorsAllowed = DEFAULT_NUMBER_OF_ERRORS_ALLOWED;
   }

   // CallbackErrorHandler implementation ----------------------------------------------------------

   public void setConfig(Map errorHandlerConfig)
   {
      if (errorHandlerConfig == null)
      {
         log.warn(this + " got null configuration");
         return;
      }

      // need to get the handler subsystem for if we need to remove the callback listener
      handlerSubsystem = (String)errorHandlerConfig.get(HANDLER_SUBSYSTEM);

      Object value = errorHandlerConfig.get(CALLBACK_ERRORS_ALLOWED);
      if (value != null)
      {
         if (value instanceof String)
         {
            String stringValue = (String) value;
            try
            {
               numOfErrorsAllowed = Integer.parseInt(stringValue);
            }
            catch (NumberFormatException e)
            {
               log.warn(this + " could not convert configuration value for " +
                  CALLBACK_ERRORS_ALLOWED + " (" + stringValue + ") into a numeric value.");
            }
         }
         else if (value instanceof Integer)
         {
            numOfErrorsAllowed = ((Integer)value).intValue();
         }
         else
         {
            log.warn(this + " could not convert configuration value for " +
               CALLBACK_ERRORS_ALLOWED + " (" + value + ") into a numeric value. " +
               "Type of value should be either String or Integer.");
         }
      }
   }

   public synchronized void handleError(Throwable ex) throws Throwable
   {
      currentNumberOfErrors++;

      log.debug(this + " handling " + ex + ". Number of errors so far " + currentNumberOfErrors);

      if (currentNumberOfErrors < numOfErrorsAllowed)
      {
         log.debug(this + " ignoring the callback error");
         throw ex;
      }

      log.debug(this + " reached maximum number of callback errors allowed (" +
         numOfErrorsAllowed + "). Will clean up callback hander now.");

      if (serverInvoker != null)
      {
         serverInvoker.removeCallbackListener(handlerSubsystem, callbackHandler);
         callbackHandler.destroy();
      }

      // rethrowing the exception so the client application can catch it and handle it too
      throw ex;
   }

   public void setServerInvoker(ServerInvoker serverInvoker)
   {
      log.debug(this + " setting server invoker to " + serverInvoker);
      this.serverInvoker = serverInvoker;
   }

   public void setCallbackHandler(ServerInvokerCallbackHandler callbackHandler)
   {
      log.debug(this + " setting callback handler to " + callbackHandler);
      this.callbackHandler = callbackHandler;
   }

   // Public ---------------------------------------------------------------------------------------

   public String toString()
   {
      StringBuffer sb = new StringBuffer("DefaultCallbackErrorHandler[");

      if (serverInvoker == null)
      {
         sb.append("UNITIALIZED");
      }
      else
      {
         sb.append(serverInvoker);
      }

      sb.append(']');

      return sb.toString();
   }

   // Package protected ----------------------------------------------------------------------------

   // Protected ------------------------------------------------------------------------------------

   // Private --------------------------------------------------------------------------------------

   // Inner classes --------------------------------------------------------------------------------

}